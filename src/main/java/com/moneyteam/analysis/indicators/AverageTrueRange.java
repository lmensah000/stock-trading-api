package com.moneyteam.analysis.indicators;

import com.moneyteam.marketdata.model.Bar;

import java.math.BigDecimal;
import java.util.List;

/**
 * Average true range, Wilder-smoothed.
 *
 * Inputs begin at bar 1, because bar 0 has no previous close and therefore no
 * true range, so the first ATR value lands at index {@code period} rather than
 * {@code period - 1}. That off-by-one is the reason the offset is explicit in
 * {@link WilderSmoothing} rather than assumed.
 */
public final class AverageTrueRange {

    private AverageTrueRange() {
    }

    public static IndicatorSeries atr(List<Bar> bars, int period) {
        Bar.requireOldestFirst(bars);
        Indicators.requirePeriod(period);

        List<BigDecimal> trueRanges = TrueRange.rawFrom(bars);
        // offset 1: the true-range series starts at bar 1.
        return WilderSmoothing.smooth(trueRanges, period, 1, bars.size());
    }
}
