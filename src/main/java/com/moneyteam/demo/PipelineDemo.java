package com.moneyteam.demo;

import com.moneyteam.bot.backtest.BacktestReport;
import com.moneyteam.bot.backtest.BacktestRunner;
import com.moneyteam.bot.backtest.BacktestTrade;
import com.moneyteam.bot.backtest.CostModel;
import com.moneyteam.bot.broker.BrokerOrder;
import com.moneyteam.bot.broker.InMemoryBroker;
import com.moneyteam.bot.journal.ContextFidelity;
import com.moneyteam.bot.journal.ContextSnapshot;
import com.moneyteam.bot.journal.EntrySource;
import com.moneyteam.bot.journal.JournalEntry;
import com.moneyteam.bot.journal.Judgement;
import com.moneyteam.bot.journal.SnapshotCodec;
import com.moneyteam.bot.journal.analysis.Disagreement;
import com.moneyteam.bot.journal.analysis.DisagreementAnalyzer;
import com.moneyteam.bot.journal.analysis.DisagreementReport;
import com.moneyteam.bot.journal.analysis.RuleBook;
import com.moneyteam.bot.journal.analysis.RuleConsistencyReport;
import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.marketdata.provider.csv.CsvMarketDataProvider;
import com.moneyteam.strategy.engine.ScanResult;
import com.moneyteam.strategy.engine.StrategyEngine;
import com.moneyteam.strategy.indicator.FairValueGapDetector;
import com.moneyteam.strategy.model.FairValueGap;
import com.moneyteam.strategy.model.GapSource;
import com.moneyteam.strategy.model.Signal;
import com.moneyteam.strategy.rule.PercentMoveRule;
import com.moneyteam.strategy.rule.StrategyRule;
import com.moneyteam.trading.model.enums.OrderSide;
import com.moneyteam.trading.service.PositionSizer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A runnable walkthrough of the analytical pipeline.
 *
 * Calls the real production classes throughout. A demo that reimplemented any
 * of this would demonstrate the demo rather than the system, so every number
 * printed below comes out of the same code the application runs.
 *
 * <h2>What it deliberately does not cover</h2>
 * The HTTP API, Flyway and anything touching a repository need a real MySQL.
 * {@code LiveTradingGate} in particular reads three repositories, and faking
 * those so a demo could print a reassuring line about real money being locked
 * would be precisely the wrong thing to fake - it is covered by
 * {@code LiveTradingGateTest}. Run {@code docker compose up} for the rest; see
 * {@code docs/DEMO.md}.
 *
 * Not a Spring bean: a plain class with a {@code main}, carrying no annotation,
 * so nothing here is ever instantiated by the application.
 */
public final class PipelineDemo {

    private static final BigDecimal EQUITY = new BigDecimal("100000");
    private static final BigDecimal ONE_PERCENT = BigDecimal.ONE;
    private static final String TICKER = "DEMO";

    private PipelineDemo() {
    }

    public static void main(String[] args) throws Exception {
        Path workDir = Path.of(args.length > 0 ? args[0] : "target/demo");
        Files.createDirectories(workDir);

        header();

        List<Bar> bars = section1Bars(workDir);
        section2Signals(workDir);
        section3Gaps(bars);
        section4Sizing();
        section5Backtest(bars);
        section6Broker();
        section7ArticulationGap();

        footer();
    }

    private static void header() {
        rule();
        System.out.println("  TCKR - analytical pipeline walkthrough");
        rule();
        System.out.println("""
                Every section below runs the real production classes.

                >>> THE BAR SERIES IS GENERATED, NOT MARKET DATA. <<<
                The R figures in section 5 demonstrate that the machinery works.
                They are NOT a backtest result and are NOT evidence about the
                strategy. No backtest has been run over real history, so the rule
                thresholds remain untested guesses.

                Not covered here: the HTTP API, Flyway and the live-trading gate.
                Those need MySQL. See docs/DEMO.md.
                """);
    }

    // ---------------------------------------------------------------
    //  1. Bars, through the real CSV parser
    // ---------------------------------------------------------------
    private static List<Bar> section1Bars(Path workDir) throws IOException {
        section("1. Market data - CsvMarketDataProvider");

        Path csv = workDir.resolve(TICKER + "_D1.csv");
        Files.writeString(csv, syntheticCsv(120));

        // Read back through the production parser, so its oldest-first guard
        // and its exact-decimal parsing are both in the path.
        List<Bar> bars = new CsvMarketDataProvider(workDir).getAllBars(TICKER, Timeframe.D1);

        System.out.printf("  wrote and re-read %s  (%d daily bars)%n", csv.getFileName(), bars.size());
        System.out.printf("  first  %s  close %s%n", bars.get(0).openTime(), bars.get(0).close());
        System.out.printf("  last   %s  close %s%n",
                bars.get(bars.size() - 1).openTime(), bars.get(bars.size() - 1).close());
        System.out.println("  ordering verified by Bar.requireOldestFirst - a reversed file is rejected,");
        System.out.println("  not silently sorted.");
        return bars;
    }

    /**
     * A deterministic series with occasional sharp up-moves, so the
     * percentage-move rule has something to find.
     *
     * Seeded on purpose: a demo whose numbers changed every run would be
     * impossible to talk about.
     */
    private static String syntheticCsv(int days) {
        Random random = new Random(42);
        StringBuilder csv = new StringBuilder("openTime,open,high,low,close,volume\n");

        double close = 100.0;
        Instant day = Instant.parse("2026-01-02T00:00:00Z");

        for (int i = 0; i < days; i++) {
            double drift = (random.nextDouble() - 0.45) * 1.8;
            // Every 17th bar gets a shock, which is what produces the >=4%
            // moves the rule keys on.
            double shock = i % 17 == 0 && i > 0 ? 5.5 + random.nextDouble() * 2 : 0;

            double open = close;
            double next = Math.max(5.0, open + drift + shock);
            double high = Math.max(open, next) + random.nextDouble() * 0.9;
            double low = Math.min(open, next) - random.nextDouble() * 0.9;
            long volume = 800_000 + (long) (random.nextDouble() * 2_000_000);

            csv.append("%s,%.2f,%.2f,%.2f,%.2f,%d%n".formatted(
                    day, open, high, Math.max(0.5, low), next, volume));

            close = next;
            // Weekdays only, so the series looks like sessions.
            day = day.plus(1, ChronoUnit.DAYS);
            if (day.atZone(java.time.ZoneOffset.UTC).getDayOfWeek().getValue() > 5) {
                day = day.plus(2, ChronoUnit.DAYS);
            }
        }
        return csv.toString();
    }

    // ---------------------------------------------------------------
    //  2. Signals, through the real concurrent engine
    // ---------------------------------------------------------------
    private static void section2Signals(Path workDir) {
        section("2. Strategy scan - StrategyEngine + PercentMoveRule");

        StrategyRule rule = new PercentMoveRule(
                new BigDecimal("4"), PercentMoveRule.Baseline.PREVIOUS_CLOSE,
                Timeframe.D1, 0, false);

        try (StrategyEngine engine = new StrategyEngine(
                new CsvMarketDataProvider(workDir), List.of(rule),
                List.of(Timeframe.D1), 250, 2, 2, Duration.ofSeconds(10))) {

            ScanResult result = engine.scan(List.of(TICKER), EQUITY);

            System.out.printf("  scanned %d ticker(s) in %dms, %d signal(s), %d failure(s)%n",
                    result.scanned(), result.elapsed().toMillis(),
                    result.signals().size(), result.failures().size());

            if (result.signals().isEmpty()) {
                System.out.println("  (the final bar did not clear the threshold - the rule is");
                System.out.println("   evaluated on the latest bar only, which is correct for a live scan)");
            }
            for (Signal signal : result.signals()) {
                System.out.printf("  %s  %s  %s%n", signal.type(), signal.ticker(), signal.summary());
                signal.evidence().forEach((k, v) -> System.out.printf("      %-18s %s%n", k, v));
            }
        }
        System.out.println("  evaluation is concurrent and read-only; acting on signals is not,");
        System.out.println("  and is deliberately single-threaded elsewhere.");
    }

    // ---------------------------------------------------------------
    //  3. Fair value gaps
    // ---------------------------------------------------------------
    private static void section3Gaps(List<Bar> bars) {
        section("3. Fair Value Gaps - FairValueGapDetector");

        List<FairValueGap> all = FairValueGapDetector.detect(bars, GapSource.UNDERLYING);
        List<FairValueGap> unfilled = FairValueGapDetector.detectUnfilled(bars, GapSource.UNDERLYING);

        System.out.printf("  %d gap(s) across %d bars, %d still unfilled%n",
                all.size(), bars.size(), unfilled.size());

        all.stream().limit(4).forEach(gap -> System.out.printf(
                "  %-8s %s  %s -> %s  formed %s%n",
                gap.direction(), gap.timeframe(), gap.gapLow(), gap.gapHigh(), gap.formedAt()));

        System.out.println("  a touching bar is NOT a gap - the comparison is a strict inequality,");
        System.out.println("  so the result does not depend on floating-point representation.");
    }

    // ---------------------------------------------------------------
    //  4. Sizing - the invariant made visible
    // ---------------------------------------------------------------
    private static void section4Sizing() {
        section("4. Position sizing - PositionSizer");

        BigDecimal trigger = new BigDecimal("50.00");
        BigDecimal tightStop = new BigDecimal("49.00");
        BigDecimal wideStop = new BigDecimal("45.00");

        long tight = PositionSizer.sharesForRisk(EQUITY, ONE_PERCENT, trigger, tightStop);
        long wide = PositionSizer.sharesForRisk(EQUITY, ONE_PERCENT, trigger, wideStop);

        System.out.printf("  equity %s, risking %s%%%n", EQUITY, ONE_PERCENT);
        System.out.printf("  stop at %s (risk %s/share)  ->  %d shares, %s committed%n",
                tightStop, trigger.subtract(tightStop), tight,
                PositionSizer.positionValue(tight, trigger));
        System.out.printf("  stop at %s (risk %s/share)  ->  %d shares, %s committed%n",
                wideStop, trigger.subtract(wideStop), wide,
                PositionSizer.positionValue(wide, trigger));

        BigDecimal riskedTight = trigger.subtract(tightStop).multiply(BigDecimal.valueOf(tight));
        BigDecimal riskedWide = trigger.subtract(wideStop).multiply(BigDecimal.valueOf(wide));
        System.out.printf("  amount at risk:  %s  vs  %s   <- the point: identical%n",
                riskedTight, riskedWide);
        System.out.println("  a wider stop buys fewer shares automatically. Conviction is not an input,");
        System.out.println("  and there is no parameter through which it could become one.");
    }

    // ---------------------------------------------------------------
    //  5. Backtest, with and without costs
    // ---------------------------------------------------------------
    private static void section5Backtest(List<Bar> bars) {
        section("5. Backtest - BacktestRunner  [GENERATED DATA, NOT A RESULT]");

        StrategyRule rule = new PercentMoveRule(
                new BigDecimal("4"), PercentMoveRule.Baseline.PREVIOUS_CLOSE,
                Timeframe.D1, 0, false);

        BacktestReport free = new BacktestRunner(
                List.of(rule), CostModel.free(), EQUITY, ONE_PERCENT, 30)
                .run(TICKER, Timeframe.D1, bars);

        BacktestReport costly = new BacktestRunner(
                List.of(rule), new CostModel(new BigDecimal("10"), new BigDecimal("0.005")),
                EQUITY, ONE_PERCENT, 30)
                .run(TICKER, Timeframe.D1, bars);

        System.out.printf("  zero cost     : %d trade(s), total %s R%n",
                free.trades().size(), free.totalR());
        System.out.printf("  10bps + 0.5c  : %d trade(s), total %s R%n",
                costly.trades().size(), costly.totalR());
        System.out.printf("  cost of trading: %s R%n", free.totalR().subtract(costly.totalR()));

        System.out.println();
        System.out.println("  first few trades (zero cost):");
        System.out.printf("  %-12s %-12s %-9s %-9s %-9s %s%n",
                "entry", "exit", "trigger", "stop", "R", "outcome");
        free.trades().stream().limit(5).forEach(t -> System.out.printf(
                "  %-12s %-12s %-9s %-9s %-9s %s%n",
                t.entryAt().toString().substring(0, 10), t.exitAt().toString().substring(0, 10),
                t.trigger(), t.stop(), t.rMultiple(), t.outcome()));

        long ambiguous = free.trades().stream().filter(BacktestTrade::isAmbiguous).count();
        System.out.println();
        System.out.printf("  %d trade(s) rested on the intrabar stop-first assumption%n", ambiguous);
        System.out.println("  (a bar holding both stop and target is scored a LOSS - resolving it the");
        System.out.println("   other way turns losing systems into winners on paper)");
        System.out.println();
        System.out.println("  " + free.summary());
        System.out.println();
        System.out.println("  >>> These R figures come from a generated series. They show the harness");
        System.out.println("      works. They are NOT evidence about the strategy. <<<");
    }

    // ---------------------------------------------------------------
    //  6. Broker, and the idempotency key
    // ---------------------------------------------------------------
    private static void section6Broker() {
        section("6. Broker - InMemoryBroker (BrokerPort)");

        InMemoryBroker broker = new InMemoryBroker(Clock.systemUTC(), EQUITY);
        System.out.println("  " + broker.describe());

        BrokerOrder first = broker.place(
                "tckr-demo-1", TICKER, OrderSide.BUY, new BigDecimal("166"), new BigDecimal("105.50"));
        System.out.printf("  placed  id=%s  %s %s @ %s  status=%s%n",
                first.brokerOrderId(), first.side(), first.quantity(),
                first.averageFillPrice(), first.status());

        // The recovery path: a response lost to a timeout must not become a
        // second position when the order is resubmitted.
        BrokerOrder resubmitted = broker.place(
                "tckr-demo-1", TICKER, OrderSide.BUY, new BigDecimal("166"), new BigDecimal("105.50"));

        System.out.printf("  resubmitted the same client order id -> id=%s%n",
                resubmitted.brokerOrderId());
        System.out.printf("  orders actually held: %d   <- one, not two%n", broker.placedOrders().size());
        System.out.println("  retrying an order whose response was lost is how one intended position");
        System.out.println("  becomes two. The client order id is the recovery path instead, which is");
        System.out.println("  why AlpacaBroker.place() carries no @Retry while its reads do.");
    }

    // ---------------------------------------------------------------
    //  7. The articulation gap
    // ---------------------------------------------------------------
    private static void section7ArticulationGap() {
        section("7. The articulation gap - DisagreementAnalyzer + RuleBook");

        SnapshotCodec codec = new SnapshotCodec();
        StrategyRule rule = new PercentMoveRule(
                new BigDecimal("4"), PercentMoveRule.Baseline.PREVIOUS_CLOSE,
                Timeframe.D1, 0, false);

        // Four recorded fills: one the rule would also have flagged, three it
        // would not have.
        List<JournalEntry> journal = List.of(
                fill(codec, "105.00", "100.00", null),                      // +5%  -> rule agrees
                fill(codec, "101.20", "100.00", new BigDecimal("2.4")),      // +1.2% winner
                fill(codec, "100.80", "100.00", new BigDecimal("1.1")),      // +0.8% winner
                fill(codec, "101.00", "100.00", new BigDecimal("-1.0")));    // +1.0% loser

        DisagreementReport report = new DisagreementAnalyzer(List.of(rule), codec)
                .analyse(journal, ContextFidelity.FULL_CHAIN_AT_FILL);

        System.out.println("  " + report.summary());
        System.out.println();
        for (Disagreement d : report.disagreements()) {
            System.out.printf("  %-24s %s  R=%-6s rules=%s%n",
                    d.verdict(), d.underlying(),
                    d.rMultiple() == null ? "-" : d.rMultiple(),
                    d.firedRules().isEmpty() ? "(silent)" : d.firedRules());
        }

        System.out.println();
        System.out.printf("  unexplained trades: %d, of which winners: %d%n",
                report.unexplainedTrades().size(), report.unexplainedWinners().size());
        System.out.println("  Those winners are the strongest evidence that something real is missing");
        System.out.println("  from the encoded rules - a setup traded profitably that no rule describes.");
        System.out.println("  Next step is a candidate rule and a backtest, not a new live rule.");

        // --- the mirror image: audit a stated rule -------------------
        System.out.println();
        System.out.println("  --- stated rules, audited against those same fills ---");

        RuleConsistencyReport consistency = new RuleBook(codec).check(List.of(
                RuleBook.StatedRule.of(
                        "I only buy above the previous close", "close > previousClose"),
                RuleBook.StatedRule.of(
                        "I need at least a 4% move", "changePercent >= 4")),
                journal);

        consistency.outcomes().forEach(o -> System.out.println("  " + o.describe()));
        System.out.println();
        System.out.println("  " + consistency.summary());
        System.out.println("  A rule naming an indicator - 'close > ema21' - is refused at parse time,");
        System.out.println("  because there is no indicator library yet. Refused loudly beats accepted");
        System.out.println("  and silently never evaluated.");
    }

    /** One recorded fill with a snapshot the rules can be replayed against. */
    private static JournalEntry fill(SnapshotCodec codec,
                                     String close,
                                     String previousClose,
                                     BigDecimal rMultiple) {
        Instant at = Instant.parse("2026-02-02T15:00:00Z");

        ContextSnapshot snapshot = new ContextSnapshot(
                TICKER, at,
                Map.of(Timeframe.D1, List.of(new Bar(
                        TICKER, at.truncatedTo(ChronoUnit.DAYS), Timeframe.D1,
                        new BigDecimal(previousClose), new BigDecimal(close),
                        new BigDecimal(previousClose), new BigDecimal(close), 1_500_000L))),
                new BigDecimal(previousClose), null, List.of(), EQUITY);

        JournalEntry entry = new JournalEntry(
                1L, TICKER, OrderSide.BUY, BigDecimal.ONE, new BigDecimal("2.15"),
                at, EntrySource.LIVE_CAPTURE, ContextFidelity.FULL_CHAIN_AT_FILL, at)
                .withSnapshot(codec.write(snapshot), at);

        if (rMultiple != null) {
            entry.closes(99L, rMultiple, at);
            // Recorded independently of the outcome: a winner the trader would
            // not repeat is the case a P&L-fitted model learns wrongly from.
            entry.judge(rMultiple.signum() > 0 ? Judgement.WOULD_REPEAT : Judgement.UNSURE, at);
        }
        return entry;
    }

    // ---------------------------------------------------------------

    private static void footer() {
        rule();
        System.out.println("""
                  What this did NOT show, and why

                  * The HTTP API, Flyway V1-V6, and the live-trading gate all need
                    MySQL, which this run had none of.
                  * LiveTradingGate reads three repositories. Faking those so a demo
                    could print "real money is locked" would be the wrong thing to
                    fake; LiveTradingGateTest covers it.
                  * No signal here came from real market data.

                  docs/DEMO.md has the docker compose walkthrough for the rest.
                """);
        rule();
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("-".repeat(74));
        System.out.println("  " + title);
        System.out.println("-".repeat(74));
    }

    private static void rule() {
        System.out.println("=".repeat(74));
    }
}
