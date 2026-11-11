package com.moneyteam.analysis.indicators;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * An indicator's output, aligned index-for-index with the bars it came from.
 *
 * <h2>Why warm-up is an absence and not a number</h2>
 * {@code docs/INDICATOR-SPEC.md} requires that an indicator of period {@code n}
 * produce <em>no value</em> until it has {@code n} bars, because a zero there
 * would be indistinguishable from a genuine zero reading. Returning
 * {@link Optional} makes that distinction impossible to lose: there is no
 * sentinel to mistake, and a caller cannot read a warm-up cell as data without
 * first handling the empty case.
 *
 * Index alignment is the other half of it. {@code valueAt(i)} is the value for
 * bar {@code i} of the input, not the i-th emitted value, so an indicator can
 * be compared against another of a different period without either side
 * tracking an offset.
 *
 * Immutable.
 */
public final class IndicatorSeries {

    private final List<BigDecimal> values;

    private IndicatorSeries(List<BigDecimal> values) {
        // Nulls are the internal representation of warm-up and never escape:
        // every read goes through valueAt and comes back as an Optional.
        this.values = Collections.unmodifiableList(new ArrayList<>(values));
    }

    /** Wraps values already aligned to the input, using null for warm-up. */
    static IndicatorSeries of(List<BigDecimal> alignedValues) {
        return new IndicatorSeries(alignedValues);
    }

    /** An all-warm-up series of the given length, for an input too short to use. */
    static IndicatorSeries empty(int length) {
        List<BigDecimal> values = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            values.add(null);
        }
        return new IndicatorSeries(values);
    }

    /** The value for bar {@code i}, or empty while the indicator is warming up. */
    public Optional<BigDecimal> valueAt(int index) {
        if (index < 0 || index >= values.size()) {
            return Optional.empty();
        }
        return Optional.ofNullable(values.get(index));
    }

    /** The most recent value, or empty when the series never warmed up. */
    public Optional<BigDecimal> latest() {
        for (int i = values.size() - 1; i >= 0; i--) {
            if (values.get(i) != null) {
                return Optional.of(values.get(i));
            }
        }
        return Optional.empty();
    }

    /** True when bar {@code i} carries a value. */
    public boolean isWarmedUpAt(int index) {
        return valueAt(index).isPresent();
    }

    /** The index of the first emitted value, or -1 when there is none. */
    public int firstWarmedUpIndex() {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) != null) {
                return i;
            }
        }
        return -1;
    }

    /** Length of the input this series is aligned to. */
    public int size() {
        return values.size();
    }

    /** Emitted values only, oldest-first, warm-up cells dropped. */
    public List<BigDecimal> emitted() {
        List<BigDecimal> present = new ArrayList<>();
        for (BigDecimal value : values) {
            if (value != null) {
                present.add(value);
            }
        }
        return List.copyOf(present);
    }

    @Override
    public String toString() {
        return "IndicatorSeries{size=" + values.size()
                + ", firstValueAt=" + firstWarmedUpIndex()
                + ", latest=" + latest().map(BigDecimal::toPlainString).orElse("none") + "}";
    }
}
