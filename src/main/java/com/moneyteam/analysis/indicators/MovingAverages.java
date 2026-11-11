package com.moneyteam.analysis.indicators;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Simple and exponential moving averages.
 *
 * Both emit their first value at index {@code period - 1} and nothing before
 * it, per the warm-up rule.
 */
public final class MovingAverages {

    private MovingAverages() {
    }

    /** Simple moving average over the trailing {@code period} values. */
    public static IndicatorSeries sma(List<BigDecimal> values, int period) {
        Objects.requireNonNull(values, "values");
        Indicators.requirePeriod(period);

        if (values.size() < period) {
            return IndicatorSeries.empty(values.size());
        }

        List<BigDecimal> out = new ArrayList<>(values.size());
        BigDecimal window = BigDecimal.ZERO;

        for (int i = 0; i < values.size(); i++) {
            window = window.add(values.get(i));
            if (i >= period) {
                // Rolling, so the cost does not grow with the period. The
                // subtraction is exact on BigDecimal, so unlike the same trick
                // on a double this cannot drift over a long series.
                window = window.subtract(values.get(i - period));
            }
            out.add(i < period - 1
                    ? null
                    : Indicators.emit(window.divide(BigDecimal.valueOf(period), Indicators.WORKING)));
        }
        return IndicatorSeries.of(out);
    }

    /**
     * Exponential moving average, <strong>SMA-seeded</strong>.
     *
     * The first value is the simple average of the first {@code period} values,
     * emitted at index {@code period - 1}; thereafter
     * {@code EMA_i = (price_i - EMA_{i-1}) * k + EMA_{i-1}} with
     * {@code k = 2/(period+1)}.
     *
     * The alternative seeding - starting from the first price - converges to the
     * same curve but differs materially for roughly {@code 3 * period} bars. On
     * a 50-period EMA that is most of a trading year, long enough to change
     * what the strategy buys, which is why the spec fixes it and this says so.
     */
    public static IndicatorSeries ema(List<BigDecimal> values, int period) {
        Objects.requireNonNull(values, "values");
        Indicators.requirePeriod(period);

        if (values.size() < period) {
            return IndicatorSeries.empty(values.size());
        }

        BigDecimal k = BigDecimal.valueOf(2)
                .divide(BigDecimal.valueOf(period + 1L), Indicators.WORKING);

        List<BigDecimal> out = new ArrayList<>(values.size());
        BigDecimal previous = null;

        for (int i = 0; i < values.size(); i++) {
            if (i < period - 1) {
                out.add(null);
                continue;
            }
            if (i == period - 1) {
                BigDecimal seed = BigDecimal.ZERO;
                for (int j = 0; j < period; j++) {
                    seed = seed.add(values.get(j));
                }
                previous = seed.divide(BigDecimal.valueOf(period), Indicators.WORKING);
            } else {
                previous = values.get(i).subtract(previous)
                        .multiply(k, Indicators.WORKING)
                        .add(previous);
            }
            // previous is kept unrounded and carried forward; only the copy
            // that leaves is rounded.
            out.add(Indicators.emit(previous));
        }
        return IndicatorSeries.of(out);
    }
}
