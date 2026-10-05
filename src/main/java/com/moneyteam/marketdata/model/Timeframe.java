package com.moneyteam.marketdata.model;

import java.time.Duration;

/**
 * Bar intervals the strategy engine understands.
 *
 * The three intraday timeframes are the ones the signal rules confirm across:
 * agreement between them is what separates a real imbalance from noise on a
 * single chart.
 */
public enum Timeframe {

    M5(Duration.ofMinutes(5)),
    M15(Duration.ofMinutes(15)),
    H1(Duration.ofHours(1)),
    D1(Duration.ofDays(1)),
    W1(Duration.ofDays(7));

    private final Duration duration;

    Timeframe(Duration duration) {
        this.duration = duration;
    }

    public Duration duration() {
        return duration;
    }

    /** The three intraday timeframes the option rules confirm across. */
    public static Timeframe[] intraday() {
        return new Timeframe[]{M5, M15, H1};
    }
}
