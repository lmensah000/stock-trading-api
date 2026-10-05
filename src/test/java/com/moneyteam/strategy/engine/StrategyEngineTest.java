package com.moneyteam.strategy.engine;

import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.OptionQuote;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.marketdata.service.MarketDataProvider;
import com.moneyteam.marketdata.service.impl.InMemoryMarketDataProvider;
import com.moneyteam.strategy.model.Signal;
import com.moneyteam.strategy.rule.PercentMoveRule;
import com.moneyteam.strategy.rule.StrategyRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The engine's concurrency behaviour.
 *
 * These tests exist because the failure modes of a parallel scan are quiet
 * ones: a ticker silently dropped, output that reorders between runs, one slow
 * name holding the whole scan open. None of those surface as an exception.
 */
class StrategyEngineTest {

    private static final Instant NOW = Instant.parse("2024-03-04T19:00:00Z");

    private static Bar bar(String ticker, int index, String open, String close) {
        BigDecimal o = new BigDecimal(open);
        BigDecimal c = new BigDecimal(close);
        return new Bar(ticker, NOW.minus(60L - index * 5L, ChronoUnit.MINUTES), Timeframe.M5,
                o, o.max(c), o.min(c), c, 10_000L);
    }

    /** A provider where every ticker has moved +5% from its previous close. */
    private static InMemoryMarketDataProvider providerFor(List<String> tickers) {
        InMemoryMarketDataProvider provider = new InMemoryMarketDataProvider();
        for (String ticker : tickers) {
            provider.putBars(ticker, Timeframe.M5, List.of(bar(ticker, 0, "101", "105")));
            provider.putPreviousClose(ticker, new BigDecimal("100"));
        }
        return provider;
    }

    private static StrategyEngine engine(MarketDataProvider provider, int workers) {
        return new StrategyEngine(
                provider,
                List.of(PercentMoveRule.upFromPreviousClose(new BigDecimal("4"))),
                List.of(Timeframe.M5),
                50,
                workers,
                4,
                Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("every ticker in the universe is evaluated")
    void scansWholeUniverse() {
        List<String> universe = IntStream.range(0, 50).mapToObj(i -> "TICK" + i).toList();

        try (StrategyEngine engine = engine(providerFor(universe), 8)) {
            ScanResult result = engine.scan(universe, new BigDecimal("100000"));

            assertThat(result.scanned()).isEqualTo(50);
            assertThat(result.signals()).hasSize(50);
            assertThat(result.failures()).isEmpty();
            assertThat(result.completeness()).isEqualTo(1.0);
        }
    }

    @Test
    @DisplayName("parallel output is identical to single-threaded output, every run")
    void outputIsDeterministic() {
        List<String> universe = IntStream.range(0, 40).mapToObj(i -> "TICK" + i).toList();
        InMemoryMarketDataProvider provider = providerFor(universe);

        List<String> serial;
        try (StrategyEngine single = engine(provider, 1)) {
            serial = keysOf(single.scan(universe, new BigDecimal("100000")));
        }

        // Repeat the parallel scan: a single run could pass by luck.
        for (int attempt = 0; attempt < 5; attempt++) {
            try (StrategyEngine parallel = engine(provider, 8)) {
                assertThat(keysOf(parallel.scan(universe, new BigDecimal("100000"))))
                        .as("attempt %d matches the single-threaded ordering", attempt)
                        .isEqualTo(serial);
            }
        }
    }

    @Test
    @DisplayName("a ticker whose data access throws is reported, and the scan continues")
    void oneFailingTickerDoesNotFailTheScan() {
        List<String> universe = List.of("GOOD1", "BOOM", "GOOD2");
        InMemoryMarketDataProvider delegate = providerFor(List.of("GOOD1", "GOOD2"));

        MarketDataProvider exploding = new MarketDataProvider() {
            @Override
            public List<Bar> getBars(String ticker, Timeframe timeframe, int lookback) {
                if ("BOOM".equals(ticker)) {
                    throw new IllegalStateException("provider exploded");
                }
                return delegate.getBars(ticker, timeframe, lookback);
            }

            @Override
            public List<OptionQuote> getOptionChain(String ticker) {
                return List.of();
            }

            @Override
            public List<Bar> getOptionBars(String key, Timeframe timeframe, int lookback) {
                return List.of();
            }

            @Override
            public BigDecimal getPreviousClose(String ticker) {
                return delegate.getPreviousClose(ticker);
            }
        };

        try (StrategyEngine engine = engine(exploding, 4)) {
            ScanResult result = engine.scan(universe, new BigDecimal("100000"));

            // The two healthy tickers still produced signals...
            assertThat(result.signals()).hasSize(2);
            // ...and the failure is surfaced rather than swallowed.
            assertThat(result.failures()).containsKey("BOOM");
            assertThat(result.failures().get("BOOM")).contains("provider exploded");
            assertThat(result.completeness()).isLessThan(1.0);
        }
    }

    @Test
    @DisplayName("a rule that throws costs only that rule, not the ticker or the scan")
    void misbehavingRuleIsContained() {
        List<String> universe = List.of("AAA", "BBB");
        StrategyRule explodes = new StrategyRule() {
            @Override
            public Optional<Signal> evaluate(com.moneyteam.strategy.rule.EvaluationContext context) {
                throw new IllegalStateException("bad rule");
            }

            @Override
            public String name() {
                return "ExplodingRule";
            }
        };

        try (StrategyEngine engine = new StrategyEngine(
                providerFor(universe),
                List.of(explodes, PercentMoveRule.upFromPreviousClose(new BigDecimal("4"))),
                List.of(Timeframe.M5), 50, 4, 4, Duration.ofSeconds(5))) {

            ScanResult result = engine.scan(universe, new BigDecimal("100000"));

            // The healthy rule still fired for both tickers.
            assertThat(result.signals()).hasSize(2);
            // The broken rule is reported per ticker.
            assertThat(result.failures()).containsKeys("AAA/ExplodingRule", "BBB/ExplodingRule");
        }
    }

    @Test
    @DisplayName("concurrent provider access is capped by the permit count")
    void providerConcurrencyIsBounded() {
        List<String> universe = IntStream.range(0, 40).mapToObj(i -> "TICK" + i).toList();
        InMemoryMarketDataProvider delegate = providerFor(universe);

        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger peak = new AtomicInteger();

        MarketDataProvider counting = new MarketDataProvider() {
            @Override
            public List<Bar> getBars(String ticker, Timeframe timeframe, int lookback) {
                int current = inFlight.incrementAndGet();
                peak.accumulateAndGet(current, Math::max);
                try {
                    Thread.sleep(5); // widen the window so overlap is observable
                    return delegate.getBars(ticker, timeframe, lookback);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return List.of();
                } finally {
                    inFlight.decrementAndGet();
                }
            }

            @Override
            public List<OptionQuote> getOptionChain(String ticker) {
                return List.of();
            }

            @Override
            public List<Bar> getOptionBars(String key, Timeframe timeframe, int lookback) {
                return List.of();
            }

            @Override
            public BigDecimal getPreviousClose(String ticker) {
                return delegate.getPreviousClose(ticker);
            }
        };

        int permits = 3;
        try (StrategyEngine engine = new StrategyEngine(
                counting,
                List.of(PercentMoveRule.upFromPreviousClose(new BigDecimal("4"))),
                List.of(Timeframe.M5), 50, 12, permits, Duration.ofSeconds(10))) {

            engine.scan(universe, new BigDecimal("100000"));
        }

        // Twelve workers, but the semaphore holds provider access to three -
        // which is what stops a wider pool turning into rate-limit rejections.
        assertThat(peak.get())
                .as("peak concurrent provider calls stayed within the permit count")
                .isLessThanOrEqualTo(permits);
    }

    @Test
    @DisplayName("work is genuinely spread across threads, not run on one")
    void workIsDistributed() {
        List<String> universe = IntStream.range(0, 40).mapToObj(i -> "TICK" + i).toList();
        InMemoryMarketDataProvider delegate = providerFor(universe);
        Set<String> threads = ConcurrentHashMap.newKeySet();

        MarketDataProvider recording = new MarketDataProvider() {
            @Override
            public List<Bar> getBars(String ticker, Timeframe timeframe, int lookback) {
                threads.add(Thread.currentThread().getName());
                return delegate.getBars(ticker, timeframe, lookback);
            }

            @Override
            public List<OptionQuote> getOptionChain(String ticker) {
                return List.of();
            }

            @Override
            public List<Bar> getOptionBars(String key, Timeframe timeframe, int lookback) {
                return List.of();
            }

            @Override
            public BigDecimal getPreviousClose(String ticker) {
                return delegate.getPreviousClose(ticker);
            }
        };

        try (StrategyEngine engine = engine(recording, 6)) {
            engine.scan(universe, new BigDecimal("100000"));
        }

        assertThat(threads).hasSizeGreaterThan(1);
        assertThat(threads).allSatisfy(name ->
                assertThat(name).startsWith("strategy-scan-"));
    }

    @Test
    @DisplayName("an empty universe is a valid, complete scan")
    void emptyUniverse() {
        try (StrategyEngine engine = engine(new InMemoryMarketDataProvider(), 4)) {
            ScanResult result = engine.scan(List.of(), new BigDecimal("100000"));

            assertThat(result.signals()).isEmpty();
            assertThat(result.scanned()).isZero();
            assertThat(result.completeness()).isEqualTo(1.0);
        }
    }

    private static List<String> keysOf(ScanResult result) {
        List<String> keys = new ArrayList<>();
        result.signals().forEach(s -> keys.add(s.dedupeKey()));
        return keys;
    }
}
