package com.moneyteam.strategy.engine;

import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.marketdata.service.MarketDataProvider;
import com.moneyteam.strategy.model.Signal;
import com.moneyteam.strategy.rule.EvaluationContext;
import com.moneyteam.strategy.rule.StrategyRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Evaluates a set of rules across a universe of tickers, concurrently.
 *
 * <h2>Why these concurrency choices</h2>
 * <ul>
 *   <li><strong>A bounded, owned pool rather than {@code parallelStream}.</strong>
 *       Parallel streams run on the shared common ForkJoinPool, so a scan would
 *       contend with every other parallel operation in the JVM and with itself
 *       if ever nested. An owned pool also means the thread count is a tuning
 *       knob rather than a function of how many cores the host happens to have.</li>
 *   <li><strong>One task per ticker.</strong> Tickers are independent, which
 *       makes this the natural grain and removes any need for shared mutable
 *       state. Every value a task touches is immutable.</li>
 *   <li><strong>A semaphore around provider calls.</strong> Market data is the
 *       bottleneck and is rate-limited; without a gate, widening the pool
 *       simply converts throughput into HTTP 429s. The real limiter replaces
 *       this when a live provider lands.</li>
 *   <li><strong>A deadline per task.</strong> One unresponsive ticker must not
 *       hold a scan open indefinitely.</li>
 *   <li><strong>Deterministic output.</strong> Results are sorted before being
 *       returned, so a parallel scan and a single-threaded one over the same
 *       fixture produce identical output and tests stay stable.</li>
 * </ul>
 *
 * <h2>What this class does not do</h2>
 * It evaluates and reports. It raises no alerts and places no orders.
 *
 * <p>That separation is not stylistic. Evaluation parallelises safely because
 * it is read-only. Acting does not: an exposure ceiling is a global constraint,
 * and two threads checking remaining capacity at the same moment will both see
 * room for one more position. Any code that acts on these signals must do so on
 * a single thread.
 */
public final class StrategyEngine implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(StrategyEngine.class);

    private final MarketDataProvider marketData;
    private final List<StrategyRule> rules;
    private final List<Timeframe> timeframes;
    private final int barLookback;
    private final Duration perTickerTimeout;
    private final ExecutorService executor;
    private final Semaphore providerPermits;

    public StrategyEngine(MarketDataProvider marketData,
                          List<StrategyRule> rules,
                          List<Timeframe> timeframes,
                          int barLookback,
                          int workerThreads,
                          int maxConcurrentProviderCalls,
                          Duration perTickerTimeout) {

        this.marketData = Objects.requireNonNull(marketData, "marketData");
        this.rules = List.copyOf(rules);
        this.timeframes = List.copyOf(timeframes);
        this.barLookback = barLookback;
        this.perTickerTimeout = Objects.requireNonNull(perTickerTimeout, "perTickerTimeout");

        if (workerThreads < 1) {
            throw new IllegalArgumentException("workerThreads must be at least 1");
        }
        if (maxConcurrentProviderCalls < 1) {
            throw new IllegalArgumentException("maxConcurrentProviderCalls must be at least 1");
        }

        this.providerPermits = new Semaphore(maxConcurrentProviderCalls);
        this.executor = new ThreadPoolExecutor(
                workerThreads, workerThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                namedThreadFactory("strategy-scan"));
    }

    /** Sensible defaults: one worker per core capped at 8, four concurrent provider calls. */
    public static StrategyEngine withDefaults(MarketDataProvider marketData, List<StrategyRule> rules) {
        int workers = Math.min(8, Math.max(2, Runtime.getRuntime().availableProcessors()));
        return new StrategyEngine(
                marketData,
                rules,
                List.of(Timeframe.intraday()),
                120,
                workers,
                4,
                Duration.ofSeconds(10));
    }

    /**
     * Evaluates every rule against every ticker.
     *
     * Never throws for a single ticker's failure: a failing ticker is recorded
     * in {@link ScanResult#failures()} and the scan continues.
     */
    public ScanResult scan(Collection<String> universe, BigDecimal accountEquity) {
        Instant started = Instant.now();
        Map<String, String> failures = new ConcurrentHashMap<>();
        List<String> tickers = List.copyOf(universe);

        List<Future<List<Signal>>> futures = new ArrayList<>(tickers.size());
        for (String ticker : tickers) {
            futures.add(executor.submit(evaluateTicker(ticker, accountEquity, failures)));
        }

        List<Signal> signals = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            String ticker = tickers.get(i);
            try {
                signals.addAll(futures.get(i).get(perTickerTimeout.toMillis(), TimeUnit.MILLISECONDS));
            } catch (TimeoutException e) {
                futures.get(i).cancel(true);
                failures.put(ticker, "timed out after " + perTickerTimeout.toMillis() + "ms");
            } catch (ExecutionException e) {
                failures.put(ticker, describe(e.getCause()));
            } catch (InterruptedException e) {
                // Restore the flag and stop promptly rather than pressing on
                // with a scan nobody is waiting for any more.
                Thread.currentThread().interrupt();
                failures.put(ticker, "interrupted");
                break;
            }
        }

        // Sorted so a parallel scan and a serial one produce identical output.
        signals.sort(Comparator
                .comparing(Signal::ticker)
                .thenComparing(s -> s.type().name())
                .thenComparing(s -> s.dedupeKey()));

        ScanResult result = new ScanResult(
                signals, failures, tickers.size(), Duration.between(started, Instant.now()));

        if (result.hasFailures()) {
            log.warn("Scan completed with {} of {} tickers failing: {}",
                    failures.size(), tickers.size(), failures.keySet());
        }
        return result;
    }

    private Callable<List<Signal>> evaluateTicker(String ticker,
                                                  BigDecimal accountEquity,
                                                  Map<String, String> failures) {
        return () -> {
            EvaluationContext context = loadContext(ticker, accountEquity);
            List<Signal> found = new ArrayList<>();
            for (StrategyRule rule : rules) {
                try {
                    rule.evaluate(context).ifPresent(found::add);
                } catch (RuntimeException e) {
                    // One misbehaving rule must not cost the ticker its other
                    // rules, nor the scan this ticker.
                    failures.put(ticker + "/" + rule.name(), describe(e));
                    log.warn("Rule {} failed for {}", rule.name(), ticker, e);
                }
            }
            return found;
        };
    }

    /**
     * Builds the per-ticker snapshot. All provider access happens here, behind
     * the semaphore, so rules themselves never touch the network.
     */
    private EvaluationContext loadContext(String ticker, BigDecimal accountEquity) throws InterruptedException {
        providerPermits.acquire();
        try {
            EvaluationContext.Builder builder =
                    EvaluationContext.builder(ticker, Instant.now()).accountEquity(accountEquity);

            for (Timeframe timeframe : timeframes) {
                builder.bars(timeframe, marketData.getBars(ticker, timeframe, barLookback));
            }

            var chain = marketData.getOptionChain(ticker);
            builder.optionChain(chain);
            builder.previousClose(marketData.getPreviousClose(ticker));

            // Option-contract bars only for contracts that could plausibly
            // matter. Pulling the whole chain's candles would multiply provider
            // calls by the width of the chain for almost no added signal.
            chain.stream()
                    .filter(q -> q.volume() > 0)
                    .sorted(Comparator.comparingLong(q -> -q.volume()))
                    .limit(5)
                    .forEach(q -> {
                        for (Timeframe timeframe : timeframes) {
                            builder.optionBars(q.contractKey(), timeframe,
                                    marketData.getOptionBars(q.contractKey(), timeframe, barLookback));
                        }
                    });

            return builder.build();
        } finally {
            providerPermits.release();
        }
    }

    private static String describe(Throwable t) {
        if (t == null) {
            return "unknown failure";
        }
        String message = t.getMessage();
        return t.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }

    private static ThreadFactory namedThreadFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + counter.incrementAndGet());
            // Daemon so a forgotten engine cannot keep the JVM alive.
            thread.setDaemon(true);
            return thread;
        };
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
