package com.moneyteam.analysis.indicators;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Wilder's smoothing: <strong>{@code 1/n}, not {@code 2/(n+1)}</strong>.
 *
 * <pre>
 *   value_i = (value_{i-1} * (n - 1) + input_i) / n
 * </pre>
 *
 * seeded from the simple average of the first {@code n} inputs.
 *
 * <h2>Why this is its own class</h2>
 * {@code docs/INDICATOR-SPEC.md} calls using the EMA multiplier here "the single
 * most common silent error in indicator code", and it is silent for a reason:
 * Wilder's {@code 1/n} is equivalent to an EMA of period {@code 2n-1}, so the
 * mistake produces an indicator that is materially faster and still looks
 * entirely plausible on a chart. Nothing crashes and no value goes out of
 * range - RSI stays within 0..100 either way.
 *
 * RSI and ATR both need it, so it exists once. One implementation means one
 * place to get it right and one test that pins it, rather than two chances to
 * reach for the wrong constant.
 */
final class WilderSmoothing {

    private WilderSmoothing() {
    }

    /**
     * Smooths {@code inputs}, aligned to {@code alignTo} positions with the
     * first {@code offset} cells left as warm-up.
     *
     * @param inputs  the values to smooth, oldest-first, with no warm-up gap
     * @param period  Wilder's {@code n}
     * @param offset  how many leading cells of the aligned output are warm-up
     *                before {@code inputs} begins - ATR's inputs start at bar 1
     *                because the first bar has no true range
     * @param alignTo total length of the aligned output
     */
    static IndicatorSeries smooth(List<BigDecimal> inputs, int period, int offset, int alignTo) {
        List<BigDecimal> raw = smoothRaw(inputs, period, offset, alignTo);
        List<BigDecimal> emitted = new ArrayList<>(raw.size());
        for (BigDecimal value : raw) {
            emitted.add(value == null ? null : Indicators.emit(value));
        }
        return IndicatorSeries.of(emitted);
    }

    /**
     * The same recurrence, left <strong>unrounded</strong>.
     *
     * For a caller that feeds the result into further arithmetic rather than
     * emitting it. RSI divides one smoothed average by another, and rounding
     * both to scale 4 first shifts the ratio enough to move the final value -
     * which is the drift {@code docs/INDICATOR-SPEC.md} means by "intermediate
     * arithmetic is carried at full precision; only the emitted value is
     * rounded". ATR has no such step, so it uses {@link #smooth} directly.
     *
     * Returns a list aligned to {@code alignTo}, null for warm-up.
     */
    static List<BigDecimal> smoothRaw(List<BigDecimal> inputs, int period, int offset, int alignTo) {
        Indicators.requirePeriod(period);

        List<BigDecimal> warmUpOnly = new ArrayList<>(alignTo);
        for (int i = 0; i < alignTo; i++) {
            warmUpOnly.add(null);
        }
        if (inputs.size() < period) {
            return warmUpOnly;
        }

        BigDecimal n = BigDecimal.valueOf(period);
        BigDecimal nMinusOne = BigDecimal.valueOf(period - 1L);

        List<BigDecimal> out = warmUpOnly;

        // Seed: simple average of the first n inputs.
        BigDecimal seed = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            seed = seed.add(inputs.get(i));
        }
        BigDecimal previous = seed.divide(n, Indicators.WORKING);

        int firstIndex = offset + period - 1;
        if (firstIndex < alignTo) {
            out.set(firstIndex, previous);
        }

        for (int i = period; i < inputs.size(); i++) {
            // The 1/n recurrence. Not (price - prev) * 2/(n+1) + prev.
            previous = previous.multiply(nMinusOne, Indicators.WORKING)
                    .add(inputs.get(i))
                    .divide(n, Indicators.WORKING);

            int at = offset + i;
            if (at < alignTo) {
                out.set(at, previous);
            }
        }
        return out;
    }
}
