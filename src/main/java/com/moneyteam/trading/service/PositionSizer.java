package com.moneyteam.trading.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Risk-based position sizing.
 *
 * <pre>
 *   shares = (equity × riskPercent) ÷ (trigger − stop)
 * </pre>
 *
 * The consequence of that formula is the point of it: a wider stop buys fewer
 * shares, automatically. Risk per trade stays constant regardless of how
 * volatile the instrument is or how certain the setup feels, because conviction
 * is not an input. There is deliberately no parameter through which it could
 * become one.
 *
 * Pure functions, no Spring and no database, so every case is exercised with
 * hand-computed numbers.
 */
public final class PositionSizer {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private PositionSizer() {
    }

    /**
     * Shares to buy for a given risk budget and stop distance.
     *
     * @param equity       account equity
     * @param riskPercent  percentage of equity to risk, e.g. 1 for 1%
     * @param trigger      entry price
     * @param stop         stop price, strictly below the trigger for a long
     * @return whole shares, rounded down so the risk budget is never exceeded
     * @throws IllegalArgumentException when the stop is not below the trigger
     */
    public static long sharesForRisk(BigDecimal equity,
                                     BigDecimal riskPercent,
                                     BigDecimal trigger,
                                     BigDecimal stop) {

        if (equity == null || equity.signum() <= 0) {
            throw new IllegalArgumentException("equity must be positive: " + equity);
        }
        if (riskPercent == null || riskPercent.signum() <= 0) {
            throw new IllegalArgumentException("riskPercent must be positive: " + riskPercent);
        }
        if (trigger == null || stop == null) {
            throw new IllegalArgumentException("trigger and stop are both required");
        }

        BigDecimal perShareRisk = trigger.subtract(stop);
        if (perShareRisk.signum() <= 0) {
            // Without a stop below the entry there is no defined risk, so there
            // is no size that can be computed - and no trade.
            throw new IllegalArgumentException(
                    "stop " + stop + " must be below trigger " + trigger + " to define risk");
        }

        BigDecimal riskBudget = equity.multiply(riskPercent)
                .divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP);

        // Floor, not round: rounding up would exceed the risk budget.
        return riskBudget.divide(perShareRisk, 0, RoundingMode.DOWN).longValueExact();
    }

    /**
     * Cash committed at entry for a given size. Separate from risk: this is
     * what the position costs, not what is at stake if the stop is hit.
     */
    public static BigDecimal positionValue(long shares, BigDecimal trigger) {
        return trigger.multiply(BigDecimal.valueOf(shares)).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * A first target at a multiple of the risk taken. Expressed in R - multiples
     * of the amount risked - rather than dollars, matching how outcomes are
     * measured everywhere else.
     */
    public static BigDecimal firstTarget(BigDecimal trigger, BigDecimal stop, BigDecimal rMultiple) {
        BigDecimal perShareRisk = trigger.subtract(stop);
        if (perShareRisk.signum() <= 0) {
            throw new IllegalArgumentException("stop must be below trigger");
        }
        return trigger.add(perShareRisk.multiply(rMultiple)).setScale(4, RoundingMode.HALF_UP);
    }
}
