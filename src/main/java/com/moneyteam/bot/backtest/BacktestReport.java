package com.moneyteam.bot.backtest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * The result of a replay.
 *
 * <h2>Why it abstains</h2>
 * Below {@link #minimumTrades} closed trades the report returns the trade count
 * and total R but leaves win rate and expectancy null. A win rate computed from
 * eight trades is not an estimate of anything, and the specific danger is that
 * it reads like one: a 75% figure invites sizing up on what is noise.
 *
 * Returning null is deliberate rather than returning the number with a warning
 * attached. A caller has to handle the absence, where it could ignore a flag.
 *
 * <h2>Costs are recorded, not assumed</h2>
 * The {@link #costModel} that produced these figures is part of the report. A
 * total R without the cost assumptions beside it is not interpretable, and the
 * commonest way to mislead yourself is to compare a zero-cost run against a
 * realistic one without noticing.
 */
public record BacktestReport(
        List<BacktestTrade> trades,
        int barsReplayed,
        int signalsFound,
        BigDecimal totalR,
        BigDecimal maxDrawdownR,
        BigDecimal winRate,
        BigDecimal expectancyR,
        int minimumTrades,
        boolean lowSample,
        int ambiguousTrades,
        CostModel costModel) {

    private static final int SCALE = 4;

    /** Default sample floor below which statistics are withheld. */
    public static final int DEFAULT_MINIMUM_TRADES = 30;

    public static BacktestReport of(List<BacktestTrade> trades,
                                    int barsReplayed,
                                    int signalsFound,
                                    int minimumTrades,
                                    CostModel costModel) {

        List<BacktestTrade> closed = List.copyOf(trades);

        BigDecimal totalR = closed.stream()
                .map(BacktestTrade::rMultiple)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);

        boolean lowSample = closed.size() < minimumTrades;

        BigDecimal winRate = null;
        BigDecimal expectancy = null;
        if (!lowSample && !closed.isEmpty()) {
            long wins = closed.stream().filter(BacktestTrade::isWin).count();
            winRate = BigDecimal.valueOf(wins)
                    .divide(BigDecimal.valueOf(closed.size()), SCALE, RoundingMode.HALF_UP);
            expectancy = totalR.divide(BigDecimal.valueOf(closed.size()), SCALE, RoundingMode.HALF_UP);
        }

        int ambiguous = (int) closed.stream().filter(BacktestTrade::isAmbiguous).count();

        return new BacktestReport(
                closed, barsReplayed, signalsFound, totalR, maxDrawdownR(closed),
                winRate, expectancy, minimumTrades, lowSample, ambiguous, costModel);
    }

    /**
     * Largest peak-to-trough fall of the cumulative R curve.
     *
     * In R rather than currency, matching how outcomes are measured everywhere
     * else, and so the figure does not change meaning when account size does.
     */
    private static BigDecimal maxDrawdownR(List<BacktestTrade> trades) {
        BigDecimal cumulative = BigDecimal.ZERO;
        BigDecimal peak = BigDecimal.ZERO;
        BigDecimal worst = BigDecimal.ZERO;

        for (BacktestTrade trade : trades) {
            cumulative = cumulative.add(trade.rMultiple());
            if (cumulative.compareTo(peak) > 0) {
                peak = cumulative;
            }
            BigDecimal drawdown = peak.subtract(cumulative);
            if (drawdown.compareTo(worst) > 0) {
                worst = drawdown;
            }
        }
        return worst.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * A one-line summary safe to log or show, which states the sample problem
     * rather than burying it.
     */
    public String summary() {
        if (lowSample) {
            return "%d trade(s) over %d bars, total %s R. Below the %d-trade floor, so win rate "
                    + "and expectancy are withheld - this is not yet evidence of anything."
                    .formatted(trades.size(), barsReplayed, totalR, minimumTrades);
        }
        return "%d trades over %d bars: total %s R, expectancy %s R, win rate %s%s".formatted(
                trades.size(), barsReplayed, totalR, expectancyR, winRate,
                ambiguousTrades > 0
                        ? ". " + ambiguousTrades + " rested on the intrabar stop-first assumption"
                        : "");
    }
}
