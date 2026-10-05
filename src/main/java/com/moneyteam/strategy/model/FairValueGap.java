package com.moneyteam.strategy.model;

import com.moneyteam.marketdata.model.Timeframe;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A three-bar price imbalance: a range that price moved through so quickly it
 * left no overlap between the bar before and the bar after.
 *
 * Bullish: {@code bar[i-1].high < bar[i+1].low}, leaving the span between them
 * untraded on the way up. Bearish is the mirror.
 *
 * @param timeframe  chart the gap was found on
 * @param direction  which way price displaced
 * @param source     underlying chart or the option contract's own chart
 * @param gapLow     lower bound of the untraded range
 * @param gapHigh    upper bound of the untraded range
 * @param formedAt   open time of the middle bar
 * @param filled     whether price has since traded back through the range
 */
public record FairValueGap(
        Timeframe timeframe,
        Direction direction,
        GapSource source,
        BigDecimal gapLow,
        BigDecimal gapHigh,
        Instant formedAt,
        boolean filled) {

    public FairValueGap {
        Objects.requireNonNull(timeframe, "timeframe");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(gapLow, "gapLow");
        Objects.requireNonNull(gapHigh, "gapHigh");
        Objects.requireNonNull(formedAt, "formedAt");

        if (gapHigh.compareTo(gapLow) <= 0) {
            // A zero-width "gap" is two bars touching, which is not an
            // imbalance. Rejecting it here means the detector cannot
            // accidentally emit one.
            throw new IllegalArgumentException(
                    "A gap must have width: gapHigh " + gapHigh + " must exceed gapLow " + gapLow);
        }
    }

    /** Width of the untraded range. */
    public BigDecimal size() {
        return gapHigh.subtract(gapLow);
    }

    /** Marks this gap as traded back through, leaving the original untouched. */
    public FairValueGap asFilled() {
        return new FairValueGap(timeframe, direction, source, gapLow, gapHigh, formedAt, true);
    }

    /** True when {@code price} lies inside the untraded range. */
    public boolean contains(BigDecimal price) {
        return price.compareTo(gapLow) >= 0 && price.compareTo(gapHigh) <= 0;
    }
}
