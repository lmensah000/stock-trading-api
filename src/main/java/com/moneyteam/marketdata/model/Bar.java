package com.moneyteam.marketdata.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * One OHLCV price bar. Immutable, and therefore safe to share across the
 * threads that evaluate a universe concurrently without any locking.
 *
 * Prices are {@link BigDecimal} rather than {@code double} on purpose. Gap
 * detection is an exact inequality between two prices - a gap either exists or
 * it does not - and binary floating point makes that comparison dependent on
 * representation error. docs/INDICATOR-SPEC.md already fixes scale-4 decimals
 * for this math, so bars carry the same type the indicators expect.
 *
 * @param ticker   the instrument this bar belongs to
 * @param openTime start of the bar's interval
 * @param timeframe the interval width
 * @param open     first trade in the interval
 * @param high     highest trade in the interval
 * @param low      lowest trade in the interval
 * @param close    last trade in the interval
 * @param volume   contracts or shares traded in the interval
 */
public record Bar(
        String ticker,
        Instant openTime,
        Timeframe timeframe,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long volume) implements Comparable<Bar> {

    public Bar {
        Objects.requireNonNull(ticker, "ticker");
        Objects.requireNonNull(openTime, "openTime");
        Objects.requireNonNull(timeframe, "timeframe");
        Objects.requireNonNull(open, "open");
        Objects.requireNonNull(high, "high");
        Objects.requireNonNull(low, "low");
        Objects.requireNonNull(close, "close");

        if (high.compareTo(low) < 0) {
            throw new IllegalArgumentException(
                    "Bar high " + high + " is below low " + low + " for " + ticker + " at " + openTime);
        }
        if (volume < 0) {
            throw new IllegalArgumentException("Bar volume cannot be negative: " + volume);
        }
    }

    /** True when the bar closed above where it opened. */
    public boolean isUp() {
        return close.compareTo(open) > 0;
    }

    /** High minus low. Not the true range - that needs the previous close. */
    public BigDecimal range() {
        return high.subtract(low);
    }

    /**
     * Bars sort oldest-first, matching the ordering invariant every indicator
     * and detector assumes: index 0 is earliest, {@code size()-1} most recent.
     */
    @Override
    public int compareTo(Bar other) {
        return openTime.compareTo(other.openTime);
    }

    /**
     * Guards the ordering invariant at the point where a detector receives
     * bars, rather than letting a reversed series silently produce wrong
     * signals. Cheap relative to the cost of a wrong trade.
     */
    public static void requireOldestFirst(List<Bar> bars) {
        for (int i = 1; i < bars.size(); i++) {
            if (bars.get(i).openTime().isBefore(bars.get(i - 1).openTime())) {
                throw new IllegalArgumentException(
                        "Bars must be oldest-first; index " + i + " is earlier than " + (i - 1));
            }
        }
    }
}
