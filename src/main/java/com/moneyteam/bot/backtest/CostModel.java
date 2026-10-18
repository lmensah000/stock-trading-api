package com.moneyteam.bot.backtest;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Commission and slippage, applied against the position on both sides.
 *
 * Costs are not a refinement to add once the results look promising. A momentum
 * rule trades often, and at realistic frequencies commission and slippage are
 * frequently the difference between a positive and a negative expectancy. A
 * zero-cost backtest does not overstate a good system slightly; it can report a
 * losing system as a winning one.
 *
 * Slippage is signed by direction rather than always subtracted: a long buys
 * higher than it wanted and sells lower. Charging it in one direction only
 * halves it, which is the easy mistake here.
 *
 * Immutable. See {@code docs/BACKTEST-SPEC.md} for the worked arithmetic.
 */
public final class CostModel {

    private static final BigDecimal TEN_THOUSAND = BigDecimal.valueOf(10_000);
    private static final int MONEY_SCALE = 4;

    private final BigDecimal slippageBps;
    private final BigDecimal commissionPerShare;

    public CostModel(BigDecimal slippageBps, BigDecimal commissionPerShare) {
        if (slippageBps == null || slippageBps.signum() < 0) {
            throw new IllegalArgumentException("slippageBps must be zero or positive: " + slippageBps);
        }
        if (commissionPerShare == null || commissionPerShare.signum() < 0) {
            throw new IllegalArgumentException(
                    "commissionPerShare must be zero or positive: " + commissionPerShare);
        }
        this.slippageBps = slippageBps;
        this.commissionPerShare = commissionPerShare;
    }

    /**
     * No costs at all.
     *
     * Useful for isolating whether a change in results came from the rule or
     * from the cost assumptions, and for nothing else. A headline figure
     * produced with this is not a result.
     */
    public static CostModel free() {
        return new CostModel(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /** The fill price actually paid when buying at {@code intended}. */
    public BigDecimal buyFill(BigDecimal intended) {
        return intended
                .multiply(BigDecimal.ONE.add(slippageBps.divide(TEN_THOUSAND, 10, RoundingMode.HALF_UP)))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** The fill price actually received when selling at {@code intended}. */
    public BigDecimal sellFill(BigDecimal intended) {
        return intended
                .multiply(BigDecimal.ONE.subtract(slippageBps.divide(TEN_THOUSAND, 10, RoundingMode.HALF_UP)))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** Commission for one share across a full round trip - entry and exit. */
    public BigDecimal roundTripCommissionPerShare() {
        return commissionPerShare.multiply(BigDecimal.valueOf(2))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal slippageBps() {
        return slippageBps;
    }

    public BigDecimal commissionPerShare() {
        return commissionPerShare;
    }

    @Override
    public String toString() {
        return "CostModel{slippageBps=" + slippageBps
                + ", commissionPerShare=" + commissionPerShare + "}";
    }
}
