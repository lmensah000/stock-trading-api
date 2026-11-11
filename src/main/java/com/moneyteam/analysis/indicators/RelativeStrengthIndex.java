package com.moneyteam.analysis.indicators;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * RSI, Wilder-smoothed.
 *
 * Average gain and average loss are each smoothed with Wilder's {@code 1/n},
 * then {@code RSI = 100 - 100/(1 + averageGain/averageLoss)}.
 *
 * Gains and losses are differences between consecutive closes, so they begin at
 * bar 1 and the first RSI value lands at index {@code period}.
 */
public final class RelativeStrengthIndex {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private RelativeStrengthIndex() {
    }

    public static IndicatorSeries rsi(List<BigDecimal> closes, int period) {
        Objects.requireNonNull(closes, "closes");
        Indicators.requirePeriod(period);

        if (closes.size() <= period) {
            return IndicatorSeries.empty(closes.size());
        }

        List<BigDecimal> gains = new ArrayList<>(closes.size() - 1);
        List<BigDecimal> losses = new ArrayList<>(closes.size() - 1);

        for (int i = 1; i < closes.size(); i++) {
            BigDecimal change = closes.get(i).subtract(closes.get(i - 1));
            gains.add(change.signum() > 0 ? change : BigDecimal.ZERO);
            // Losses are carried positive, so the smoothing is the same shape
            // for both and the ratio below needs no sign handling.
            losses.add(change.signum() < 0 ? change.negate() : BigDecimal.ZERO);
        }

        // Raw, not emitted. Dividing two values already rounded to scale 4
        // shifts the ratio enough to move the final RSI - the exact drift the
        // spec forbids, and the vectors caught it.
        List<BigDecimal> averageGain = WilderSmoothing.smoothRaw(gains, period, 1, closes.size());
        List<BigDecimal> averageLoss = WilderSmoothing.smoothRaw(losses, period, 1, closes.size());

        List<BigDecimal> out = new ArrayList<>(closes.size());
        for (int i = 0; i < closes.size(); i++) {
            BigDecimal gain = averageGain.get(i);
            BigDecimal loss = averageLoss.get(i);

            if (gain == null || loss == null) {
                out.add(null);
                continue;
            }

            if (loss.signum() == 0) {
                // No losses in the window: RS is unbounded and RSI saturates.
                // Computing the ratio would divide by zero.
                out.add(Indicators.emit(ONE_HUNDRED));
                continue;
            }

            BigDecimal rs = gain.divide(loss, Indicators.WORKING);
            out.add(Indicators.emit(ONE_HUNDRED.subtract(
                    ONE_HUNDRED.divide(BigDecimal.ONE.add(rs), Indicators.WORKING))));
        }
        return IndicatorSeries.of(out);
    }
}
