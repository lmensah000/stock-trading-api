package com.moneyteam.strategy.model;

/**
 * Which chart a Fair Value Gap was detected on.
 *
 * The distinction matters more than it first appears. A gap on the underlying
 * is an imbalance in a liquid, continuously quoted market. A gap on an option
 * contract's own candles frequently is not: thin contracts leave many intervals
 * with no trades at all, prints hop between a wide bid and ask so consecutive
 * bars can appear to gap when no imbalance occurred, and an option's price
 * moves with implied volatility and time decay as well as order flow - so an
 * apparent gap may be decay rather than demand.
 *
 * Both are detected, because comparing them in practice is worth doing. But a
 * signal resting only on OPTION_CONTRACT evidence is capped at
 * {@link Confidence#LOW} so the difference stays visible at the point of use.
 */
public enum GapSource {
    UNDERLYING,
    OPTION_CONTRACT;

    /** The highest confidence a signal resting solely on this source may claim. */
    public Confidence confidenceCeiling() {
        return this == UNDERLYING ? Confidence.HIGH : Confidence.LOW;
    }
}
