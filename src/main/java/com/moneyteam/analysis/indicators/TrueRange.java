package com.moneyteam.analysis.indicators;

import com.moneyteam.marketdata.model.Bar;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * True range, gap-aware.
 *
 * <pre>
 *   TR_i = max(high_i - low_i, |high_i - close_{i-1}|, |low_i - close_{i-1}|)
 * </pre>
 *
 * <h2>Why the second and third terms matter</h2>
 * They are what make the measure gap-aware. Using only {@code high - low}
 * understates volatility exactly when it matters - on the gap - and because
 * position size is {@code risk / (trigger - stop)}, an understated range
 * silently <em>oversizes</em> the position. The error is in the dangerous
 * direction, and it is invisible unless you look for it.
 *
 * The first bar has no previous close and therefore no true range.
 */
public final class TrueRange {

    private TrueRange() {
    }

    /** True range per bar, with no value at index 0. */
    public static IndicatorSeries of(List<Bar> bars) {
        Bar.requireOldestFirst(bars);

        List<BigDecimal> out = new ArrayList<>(bars.size());
        for (int i = 0; i < bars.size(); i++) {
            out.add(i == 0 ? null : trueRange(bars.get(i), bars.get(i - 1).close()));
        }
        return IndicatorSeries.of(out);
    }

    /** The raw true-range values, oldest-first, excluding the absent first bar. */
    static List<BigDecimal> rawFrom(List<Bar> bars) {
        List<BigDecimal> values = new ArrayList<>(Math.max(0, bars.size() - 1));
        for (int i = 1; i < bars.size(); i++) {
            values.add(trueRange(bars.get(i), bars.get(i - 1).close()));
        }
        return values;
    }

    private static BigDecimal trueRange(Bar bar, BigDecimal previousClose) {
        BigDecimal range = bar.high().subtract(bar.low());
        BigDecimal upGap = bar.high().subtract(previousClose).abs();
        BigDecimal downGap = bar.low().subtract(previousClose).abs();
        return Indicators.emit(range.max(upGap).max(downGap));
    }
}
