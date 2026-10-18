package com.moneyteam.bot.backtest;

import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.strategy.rule.EvaluationContext;
import com.moneyteam.strategy.rule.PercentMoveRule;
import com.moneyteam.strategy.rule.StrategyRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Replay scoring, against the hand-computed values in docs/BACKTEST-SPEC.md.
 *
 * Those numbers were written before this class and before the runner. The
 * assertions therefore check the specification rather than restating whatever
 * the implementation produced - which matters most for the two decisions a
 * backtest can quietly make in its own favour: when entry happens, and what a
 * bar that touches both the stop and the target means.
 */
class BacktestRunnerTest {

    private static final BigDecimal EQUITY = new BigDecimal("100000");
    private static final BigDecimal ONE_PERCENT = BigDecimal.ONE;

    /** The fixture from the spec. */
    private static List<Bar> fixture() {
        List<Bar> bars = new ArrayList<>();
        bars.add(bar("2026-01-02T00:00:00Z", "100.00", "101.00", "99.00", "100.00"));
        bars.add(bar("2026-01-05T00:00:00Z", "100.00", "106.00", "99.50", "105.00"));
        bars.add(bar("2026-01-06T00:00:00Z", "105.50", "112.00", "105.00", "111.00"));
        bars.add(bar("2026-01-07T00:00:00Z", "111.00", "118.00", "110.00", "117.00"));
        return bars;
    }

    private static Bar bar(String time, String o, String h, String l, String c) {
        return new Bar("TEST", Instant.parse(time), Timeframe.D1,
                new BigDecimal(o), new BigDecimal(h), new BigDecimal(l), new BigDecimal(c), 1000L);
    }

    private static BacktestRunner runner(CostModel costs) {
        // upFromPreviousClose() hardcodes M5; the spec's fixture is daily, so
        // the rule is constructed directly against D1.
        StrategyRule rule = new PercentMoveRule(
                new BigDecimal("4"), PercentMoveRule.Baseline.PREVIOUS_CLOSE, Timeframe.D1, 0, false);

        return new BacktestRunner(
                List.of(rule), costs, EQUITY, ONE_PERCENT, BacktestReport.DEFAULT_MINIMUM_TRADES);
    }

    @Test
    @DisplayName("the signal fires on bar 1 and entry is at bar 2's open, not bar 1's close")
    void entersAtNextBarOpen() {
        BacktestReport report = runner(CostModel.free()).run("TEST", Timeframe.D1, fixture());

        assertThat(report.trades()).hasSize(1);
        BacktestTrade trade = report.trades().get(0);

        assertThat(trade.signalAt()).isEqualTo(Instant.parse("2026-01-05T00:00:00Z"));
        assertThat(trade.entryAt()).isEqualTo(Instant.parse("2026-01-06T00:00:00Z"));

        // Bar 2's open, not bar 1's close of 105.00. Entering at the signal
        // bar's close is the classic lookahead bug.
        assertThat(trade.trigger()).isEqualByComparingTo("105.50");
        assertThat(trade.trigger()).isNotEqualByComparingTo("105.00");
    }

    @Test
    @DisplayName("stop, risk, target and size match the spec exactly")
    void planMatchesSpec() {
        BacktestTrade trade = runner(CostModel.free())
                .run("TEST", Timeframe.D1, fixture()).trades().get(0);

        assertThat(trade.stop()).isEqualByComparingTo("99.50");        // bar 1's low
        assertThat(trade.plannedRisk()).isEqualByComparingTo("6.00");  // 105.50 - 99.50
        assertThat(trade.target()).isEqualByComparingTo("117.50");     // trigger + 2R
        assertThat(trade.shares()).isEqualTo(166);                     // floor(1000 / 6.00)
    }

    @Test
    @DisplayName("the fixture scores exactly 2.0000 R with no costs")
    void scoresTwoRWithoutCosts() {
        BacktestReport report = runner(CostModel.free()).run("TEST", Timeframe.D1, fixture());
        BacktestTrade trade = report.trades().get(0);

        assertThat(trade.outcome()).isEqualTo(BacktestTrade.Outcome.TARGET);
        assertThat(trade.exitFill()).isEqualByComparingTo("117.50");
        assertThat(trade.rMultiple()).isEqualByComparingTo("2.0000");
        assertThat(report.totalR()).isEqualByComparingTo("2.0000");
    }

    @Test
    @DisplayName("10 bps of slippage reduces it to exactly 1.9628 R")
    void costsReduceRToSpecValue() {
        CostModel tenBps = new CostModel(new BigDecimal("10"), BigDecimal.ZERO);

        BacktestTrade trade = runner(tenBps).run("TEST", Timeframe.D1, fixture()).trades().get(0);

        // 105.50 x 1.001 and 117.50 x 0.999 - slippage against the position on
        // both sides. Charging it one way only would halve it.
        assertThat(trade.entryFill()).isEqualByComparingTo("105.6055");
        assertThat(trade.exitFill()).isEqualByComparingTo("117.3825");
        assertThat(trade.realisedPerShare()).isEqualByComparingTo("11.7770");
        assertThat(trade.rMultiple()).isEqualByComparingTo("1.9628");
    }

    @Test
    @DisplayName("costs strictly reduce R, never improve it")
    void costsAreStrictlyAdverse() {
        BigDecimal free = runner(CostModel.free())
                .run("TEST", Timeframe.D1, fixture()).totalR();
        BigDecimal costly = runner(new CostModel(new BigDecimal("10"), new BigDecimal("0.005")))
                .run("TEST", Timeframe.D1, fixture()).totalR();

        assertThat(costly).isLessThan(free);
    }

    @Test
    @DisplayName("a bar holding both the stop and the target is scored as a loss")
    void ambiguousBarResolvesToTheStop() {
        List<Bar> bars = fixture();
        // Bar 3 now spans 99.00 to 118.00: it contains the stop at 99.50 and
        // the target at 117.50. OHLC cannot say which came first.
        bars.set(3, bar("2026-01-07T00:00:00Z", "111.00", "118.00", "99.00", "100.00"));

        BacktestTrade trade = runner(CostModel.free())
                .run("TEST", Timeframe.D1, bars).trades().get(0);

        assertThat(trade.outcome()).isEqualTo(BacktestTrade.Outcome.STOP_AMBIGUOUS_BAR);
        assertThat(trade.rMultiple()).isEqualByComparingTo("-1.0000");
        assertThat(trade.isWin()).isFalse();
    }

    @Test
    @DisplayName("ambiguous outcomes are counted, so their share of the results is visible")
    void ambiguousTradesAreCounted() {
        List<Bar> bars = fixture();
        bars.set(3, bar("2026-01-07T00:00:00Z", "111.00", "118.00", "99.00", "100.00"));

        BacktestReport report = runner(CostModel.free()).run("TEST", Timeframe.D1, bars);

        // If most results rest on the stop-first assumption, the timeframe is
        // too coarse to conclude anything - which the reader can only know if
        // the count is reported.
        assertThat(report.ambiguousTrades()).isEqualTo(1);
    }

    @Test
    @DisplayName("no lookahead: changing bars after the signal cannot change the signal")
    void laterBarsCannotInfluenceTheSignal() {
        List<Bar> original = fixture();
        List<Bar> altered = fixture();
        // Replace everything after the signal bar with wildly different prices.
        altered.set(2, bar("2026-01-06T00:00:00Z", "500.00", "600.00", "400.00", "550.00"));
        altered.set(3, bar("2026-01-07T00:00:00Z", "550.00", "700.00", "500.00", "650.00"));

        BacktestReport a = runner(CostModel.free()).run("TEST", Timeframe.D1, original);
        BacktestReport b = runner(CostModel.free()).run("TEST", Timeframe.D1, altered);

        // The signal is identical because the rule never saw those bars; only
        // the resulting fills differ.
        assertThat(a.signalsFound()).isEqualTo(b.signalsFound());
        assertThat(a.trades().get(0).signalAt()).isEqualTo(b.trades().get(0).signalAt());
        assertThat(a.trades().get(0).stop()).isEqualByComparingTo(b.trades().get(0).stop());
    }

    @Test
    @DisplayName("the rule is handed only bars up to the current index")
    void contextIsTruncatedAtTheCurrentBar() {
        List<Integer> windowSizes = new ArrayList<>();
        StrategyRule spy = new StrategyRule() {
            @Override
            public Optional<com.moneyteam.strategy.model.Signal> evaluate(EvaluationContext context) {
                windowSizes.add(context.bars(Timeframe.D1).size());
                return Optional.empty();
            }

            @Override
            public String name() {
                return "window-spy";
            }
        };

        new BacktestRunner(List.of(spy), CostModel.free(), EQUITY, ONE_PERCENT, 30)
                .run("TEST", Timeframe.D1, fixture());

        // Steps 0, 1, 2 -> windows of 1, 2, 3. The final bar is never a signal
        // bar because there is no following bar to enter on.
        assertThat(windowSizes).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("no signal on bar 0: there is no previous close to measure from")
    void firstBarHasNoBaseline() {
        BacktestReport report = runner(CostModel.free()).run("TEST", Timeframe.D1, fixture());

        assertThat(report.signalsFound()).isEqualTo(1);
        assertThat(report.trades().get(0).signalAt())
                .isNotEqualTo(Instant.parse("2026-01-02T00:00:00Z"));
    }

    @Test
    @DisplayName("a position still open when data runs out is marked, not discarded")
    void openPositionAtEndOfDataIsRecorded() {
        List<Bar> bars = fixture();
        // Bar 3 reaches neither stop nor target.
        bars.set(3, bar("2026-01-07T00:00:00Z", "111.00", "112.00", "110.00", "111.00"));

        BacktestReport report = runner(CostModel.free()).run("TEST", Timeframe.D1, bars);

        // Dropping these would silently remove whichever trades were losing at
        // the end of the series.
        assertThat(report.trades()).hasSize(1);
        assertThat(report.trades().get(0).outcome()).isEqualTo(BacktestTrade.Outcome.END_OF_DATA);
        assertThat(report.trades().get(0).exitFill()).isEqualByComparingTo("111.00");
    }

    @Test
    @DisplayName("out-of-order bars are rejected rather than sorted")
    void outOfOrderBarsAreRejected() {
        List<Bar> reversed = List.of(
                bar("2026-01-05T00:00:00Z", "100.00", "106.00", "99.50", "105.00"),
                bar("2026-01-02T00:00:00Z", "100.00", "101.00", "99.00", "100.00"));

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> runner(CostModel.free()).run("TEST", Timeframe.D1, reversed))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oldest-first");
    }

    @Test
    @DisplayName("a series too short to enter on produces nothing rather than failing")
    void singleBarSeriesIsSafe() {
        BacktestReport report = runner(CostModel.free())
                .run("TEST", Timeframe.D1, List.of(fixture().get(0)));

        assertThat(report.trades()).isEmpty();
        assertThat(report.signalsFound()).isZero();
        assertThat(report.barsReplayed()).isEqualTo(1);
    }

    @Test
    @DisplayName("end to end: the committed CSV replays to the spec's 2.0000 R")
    void endToEndFromTheCommittedCsv() {
        // File on disk -> provider -> runner, so the fixture, the spec and the
        // implementation are all held together by one assertion. The in-code
        // fixture above could drift from the CSV without this.
        List<Bar> fromDisk = new com.moneyteam.marketdata.provider.csv.CsvMarketDataProvider(
                java.nio.file.Path.of("src/test/resources/backtest"))
                .getAllBars("TEST", Timeframe.D1);

        BacktestReport report = runner(CostModel.free()).run("TEST", Timeframe.D1, fromDisk);

        assertThat(report.barsReplayed()).isEqualTo(4);
        assertThat(report.trades()).hasSize(1);
        assertThat(report.totalR()).isEqualByComparingTo("2.0000");
        assertThat(report.trades().get(0).outcome()).isEqualTo(BacktestTrade.Outcome.TARGET);

        // One trade is far below the floor, so the report must withhold
        // statistics rather than claim a 100% win rate.
        assertThat(report.lowSample()).isTrue();
        assertThat(report.winRate()).isNull();
    }

    @Test
    @DisplayName("the committed unsorted fixture is rejected by the provider")
    void committedUnsortedFixtureIsRejected() {
        var provider = new com.moneyteam.marketdata.provider.csv.CsvMarketDataProvider(
                java.nio.file.Path.of("src/test/resources/backtest"));

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> provider.getAllBars("UNSORTED", Timeframe.D1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oldest-first");
    }

    @Test
    @DisplayName("at least one rule is required")
    void rulesCannotBeEmpty() {
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> new BacktestRunner(
                        List.of(), CostModel.free(), EQUITY, ONE_PERCENT, 30))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
