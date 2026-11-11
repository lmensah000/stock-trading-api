package com.moneyteam.analysis.indicators;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Shared precision rules for every indicator.
 *
 * {@code docs/INDICATOR-SPEC.md}: intermediate arithmetic is carried at full
 * precision and only the emitted value is rounded, to scale 4 HALF_UP. Rounding
 * at each step accumulates drift that surfaces several bars later, in a
 * recurrence like Wilder's where each value feeds the next - which is precisely
 * where it is hardest to notice and hardest to trace.
 */
final class Indicators {

    /** Scale of an emitted value. */
    static final int SCALE = 4;

    /** Working precision for intermediate division. Well beyond the emitted scale. */
    static final MathContext WORKING = new MathContext(20, RoundingMode.HALF_UP);

    private Indicators() {
    }

    /** Rounds a value for emission. Applied once, at the end. */
    static BigDecimal emit(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    static void requirePeriod(int period) {
        if (period < 1) {
            throw new IllegalArgumentException("period must be at least 1, got " + period);
        }
    }
}
