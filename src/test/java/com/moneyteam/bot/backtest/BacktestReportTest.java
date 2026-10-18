package com.moneyteam.bot.backtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Report aggregation, and the abstention that keeps it honest.
 *
 * The behaviour worth protecting is the refusal to quote a win rate from a
 * handful of trades. A 75% figure from eight trades reads exactly like a 75%
 * figure from eight hundred, and acting on the first is how a run of luck
 * becomes a sizing decision.
 */
class BacktestReportTest {

    private static BacktestTrade trade(String r) {
        BigDecimal rMultiple = new BigDecimal(r);
        return new BacktestTrade(
                "TEST", Instant.EPOCH, Instant.EPOCH, Instant.EPOCH,
                new BigDecimal("100"), new BigDecimal("99"), new BigDecimal("102"), 100L,
                new BigDecimal("100"), new BigDecimal("102"), BigDecimal.ONE,
                rMultiple, rMultiple,
                rMultiple.signum() > 0 ? BacktestTrade.Outcome.TARGET : BacktestTrade.Outcome.STOP);
    }

    private static List<BacktestTrade> repeat(String r, int count) {
        List<BacktestTrade> trades = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            trades.add(trade(r));
        }
        return trades;
    }

    @Test
    @DisplayName("below the sample floor, win rate and expectancy are withheld entirely")
    void abstainsBelowTheSampleFloor() {
        BacktestReport report = BacktestReport.of(repeat("2.0", 5), 100, 5, 30, CostModel.free());

        assertThat(report.lowSample()).isTrue();
        // Null, not zero and not the real figure with a flag beside it: a
        // caller has to handle an absence, where it could ignore a boolean.
        assertThat(report.winRate()).isNull();
        assertThat(report.expectancyR()).isNull();

        // The facts that do not require a sample are still reported.
        assertThat(report.totalR()).isEqualByComparingTo("10.0000");
        assertThat(report.trades()).hasSize(5);
    }

    @Test
    @DisplayName("at the sample floor the statistics appear")
    void reportsStatisticsAtTheFloor() {
        BacktestReport report = BacktestReport.of(repeat("2.0", 30), 500, 30, 30, CostModel.free());

        assertThat(report.lowSample()).isFalse();
        assertThat(report.winRate()).isEqualByComparingTo("1.0000");
        assertThat(report.expectancyR()).isEqualByComparingTo("2.0000");
    }

    @Test
    @DisplayName("win rate and expectancy are computed over all closed trades")
    void computesWinRateAndExpectancy() {
        List<BacktestTrade> trades = new ArrayList<>(repeat("2.0", 15));
        trades.addAll(repeat("-1.0", 15));

        BacktestReport report = BacktestReport.of(trades, 500, 30, 30, CostModel.free());

        // 15 wins of 30 -> 0.5; total R = 30 - 15 = 15; expectancy = 0.5
        assertThat(report.winRate()).isEqualByComparingTo("0.5000");
        assertThat(report.totalR()).isEqualByComparingTo("15.0000");
        assertThat(report.expectancyR()).isEqualByComparingTo("0.5000");
    }

    @Test
    @DisplayName("max drawdown is the worst peak-to-trough fall of the R curve")
    void computesMaxDrawdown() {
        // Curve: +3, +1, -2, +2  ->  cumulative 3, 4, 2, 4
        // Peak 4 at trade 2, trough 2 at trade 3 -> drawdown 2.
        List<BacktestTrade> trades = List.of(
                trade("3.0"), trade("1.0"), trade("-2.0"), trade("2.0"));

        BacktestReport report = BacktestReport.of(trades, 100, 4, 1, CostModel.free());

        assertThat(report.maxDrawdownR()).isEqualByComparingTo("2.0000");
        assertThat(report.totalR()).isEqualByComparingTo("4.0000");
    }

    @Test
    @DisplayName("a run that only ever gains has no drawdown")
    void monotonicCurveHasNoDrawdown() {
        BacktestReport report = BacktestReport.of(repeat("1.0", 5), 100, 5, 1, CostModel.free());

        assertThat(report.maxDrawdownR()).isEqualByComparingTo("0.0000");
    }

    @Test
    @DisplayName("an empty run reports zero rather than failing")
    void emptyRunIsSafe() {
        BacktestReport report = BacktestReport.of(List.of(), 50, 0, 30, CostModel.free());

        assertThat(report.trades()).isEmpty();
        assertThat(report.totalR()).isEqualByComparingTo("0.0000");
        assertThat(report.maxDrawdownR()).isEqualByComparingTo("0.0000");
        assertThat(report.lowSample()).isTrue();
    }

    @Test
    @DisplayName("the summary of a low-sample run says so instead of quoting figures")
    void summaryStatesTheSampleProblem() {
        String summary = BacktestReport.of(repeat("2.0", 3), 100, 3, 30, CostModel.free()).summary();

        assertThat(summary)
                .contains("withheld")
                .doesNotContain("win rate 1");
    }

    @Test
    @DisplayName("the cost model that produced the figures travels with them")
    void costModelIsPartOfTheReport() {
        CostModel costs = new CostModel(new BigDecimal("10"), new BigDecimal("0.005"));

        BacktestReport report = BacktestReport.of(repeat("2.0", 1), 10, 1, 30, costs);

        // A total R without its cost assumptions beside it is not interpretable.
        assertThat(report.costModel().slippageBps()).isEqualByComparingTo("10");
        assertThat(report.costModel().commissionPerShare()).isEqualByComparingTo("0.005");
    }
}
