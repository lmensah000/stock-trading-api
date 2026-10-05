package com.moneyteam.marketdata.aggregate;

import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.Timeframe;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds wider bars from narrower ones.
 *
 * <h2>Why this exists</h2>
 * Schwab offers minute widths of 1, 5, 10, 15 and 30, plus daily, weekly and
 * monthly. There is <strong>no one-hour frequency</strong>. The Fair Value Gap
 * rule confirms across 5m, 15m and 1h, so 1h bars have to be built here from
 * 30-minute bars rather than requested.
 *
 * <h2>The boundary decision</h2>
 * Two 30-minute bars make an hour, but <em>which</em> two is a choice, and the
 * choice changes which gaps the detector finds. A US equity session opens at
 * 09:30 ET, so clock-aligned hours (10:00-11:00) would make the first bar of
 * every day a half-bar, and every subsequent bar straddle the one before it on
 * the previous convention.
 *
 * This aggregator pairs bars <strong>from the start of the supplied series</strong>:
 * the first two input bars become the first output bar, the next two the second,
 * and so on. Given a series that begins at the session open, hours therefore run
 * 09:30-10:30, 10:30-11:30 and so on - aligned to the session rather than to the
 * wall clock. That is the convention the FVG vectors assume, and it is recorded
 * here rather than left to be inferred from behaviour.
 *
 * <h2>Trailing partial groups</h2>
 * A series with an odd number of 30-minute bars ends with a half-formed hour.
 * It is <strong>emitted</strong>, not discarded: the bar currently forming is
 * the one being traded, and dropping it would hide the most recent price action
 * from every rule. Its {@code volume} and range reflect only the bars that
 * exist so far, which is correct - it is a partial bar, not a wrong one.
 *
 * Pure and stateless; safe to call from the scan threads.
 */
public final class BarAggregator {

    private BarAggregator() {
    }

    /**
     * Aggregates 30-minute bars into one-hour bars.
     *
     * @param thirtyMinuteBars oldest-first 30-minute series
     * @return oldest-first hourly series; empty when the input is empty
     */
    public static List<Bar> toHourly(List<Bar> thirtyMinuteBars) {
        return aggregate(thirtyMinuteBars, 2, Timeframe.H1);
    }

    /**
     * Groups consecutive bars {@code groupSize} at a time.
     *
     * open  = first bar's open
     * high  = highest high in the group
     * low   = lowest low in the group
     * close = last bar's close
     * volume= sum of the group
     */
    public static List<Bar> aggregate(List<Bar> bars, int groupSize, Timeframe target) {
        if (bars == null || bars.isEmpty()) {
            return List.of();
        }
        if (groupSize < 2) {
            throw new IllegalArgumentException("groupSize must be at least 2: " + groupSize);
        }
        Bar.requireOldestFirst(bars);

        List<Bar> out = new ArrayList<>((bars.size() / groupSize) + 1);

        for (int start = 0; start < bars.size(); start += groupSize) {
            int end = Math.min(start + groupSize, bars.size());
            out.add(combine(bars.subList(start, end), target));
        }
        return List.copyOf(out);
    }

    private static Bar combine(List<Bar> group, Timeframe target) {
        Bar first = group.get(0);
        Bar last = group.get(group.size() - 1);

        var high = first.high();
        var low = first.low();
        long volume = 0;

        for (Bar bar : group) {
            if (bar.high().compareTo(high) > 0) {
                high = bar.high();
            }
            if (bar.low().compareTo(low) < 0) {
                low = bar.low();
            }
            volume += bar.volume();
        }

        return new Bar(
                first.ticker(),
                first.openTime(),   // the group opens when its first bar opened
                target,
                first.open(),
                high,
                low,
                last.close(),
                volume);
    }

    /**
     * True when the final group is incomplete, i.e. the last emitted bar is
     * still forming. Callers that need only closed bars can drop it; the
     * strategy rules deliberately keep it.
     */
    public static boolean lastGroupIsPartial(List<Bar> source, int groupSize) {
        return !source.isEmpty() && source.size() % groupSize != 0;
    }

    /** Sanity check that a series really is the width it claims. */
    public static boolean spacingMatches(List<Bar> bars, Duration expected) {
        for (int i = 1; i < bars.size(); i++) {
            Instant previous = bars.get(i - 1).openTime();
            Instant current = bars.get(i).openTime();
            if (!Duration.between(previous, current).equals(expected)) {
                return false;
            }
        }
        return true;
    }
}
