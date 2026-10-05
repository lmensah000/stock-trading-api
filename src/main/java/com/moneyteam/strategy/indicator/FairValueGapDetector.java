package com.moneyteam.strategy.indicator;

import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.strategy.model.Direction;
import com.moneyteam.strategy.model.FairValueGap;
import com.moneyteam.strategy.model.GapSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects Fair Value Gaps: three-bar imbalances where price displaced far
 * enough that the first and third bars share no overlap.
 *
 * <pre>
 *   Bullish                        Bearish
 *   bar[i-1].high &lt; bar[i+1].low   bar[i-1].low &gt; bar[i+1].high
 *   gap = (bar[i-1].high, bar[i+1].low)
 * </pre>
 *
 * <h2>Decisions this encodes</h2>
 * <ul>
 *   <li><strong>Touching is not a gap.</strong> The comparison is strict. When
 *       {@code bar[i-1].high == bar[i+1].low} the bars meet exactly and no
 *       range went untraded. Using {@code <=} here is the single easiest way to
 *       write a detector that looks right and fires constantly.</li>
 *   <li><strong>Bars are oldest-first</strong>, checked rather than assumed. A
 *       reversed series would turn every bullish gap into a bearish one and
 *       report it confidently.</li>
 *   <li><strong>Filled gaps are tracked, not dropped.</strong> Once price trades
 *       back through the range the imbalance has been rebalanced. The gap is
 *       marked {@code filled} rather than deleted, so a caller can still see it
 *       happened; {@link #detectUnfilled} is the usual entry point.</li>
 * </ul>
 *
 * Stateless and side-effect free, so it is safe to call from the threads that
 * evaluate a universe concurrently.
 */
public final class FairValueGapDetector {

    /** A gap needs a bar either side of the displacement bar. */
    public static final int MINIMUM_BARS = 3;

    private FairValueGapDetector() {
    }

    /**
     * Every gap in the series, including those price has since filled.
     *
     * @param bars   price bars, oldest-first
     * @param source which chart these bars came from
     * @return gaps in formation order; empty when fewer than three bars
     */
    public static List<FairValueGap> detect(List<Bar> bars, GapSource source) {
        if (bars == null || bars.size() < MINIMUM_BARS) {
            return List.of();
        }
        Bar.requireOldestFirst(bars);

        Timeframe timeframe = bars.get(0).timeframe();
        List<FairValueGap> gaps = new ArrayList<>();

        for (int i = 1; i < bars.size() - 1; i++) {
            Bar before = bars.get(i - 1);
            Bar middle = bars.get(i);
            Bar after = bars.get(i + 1);

            // Strict inequality throughout: bars that merely touch leave no
            // untraded range, so they are not an imbalance.
            if (before.high().compareTo(after.low()) < 0) {
                gaps.add(gap(timeframe, Direction.BULLISH, source,
                        before.high(), after.low(), middle, bars, i));
            } else if (before.low().compareTo(after.high()) > 0) {
                gaps.add(gap(timeframe, Direction.BEARISH, source,
                        after.high(), before.low(), middle, bars, i));
            }
        }
        return List.copyOf(gaps);
    }

    /**
     * Gaps price has not yet traded back through - the ones that still
     * represent an unrebalanced imbalance, and so the ones worth acting on.
     */
    public static List<FairValueGap> detectUnfilled(List<Bar> bars, GapSource source) {
        return detect(bars, source).stream().filter(g -> !g.filled()).toList();
    }

    private static FairValueGap gap(Timeframe timeframe,
                                    Direction direction,
                                    GapSource source,
                                    BigDecimal low,
                                    BigDecimal high,
                                    Bar middle,
                                    List<Bar> bars,
                                    int middleIndex) {
        boolean filled = isFilledAfter(bars, middleIndex, low, high);
        return new FairValueGap(timeframe, direction, source, low, high, middle.openTime(), filled);
    }

    /**
     * A gap counts as filled once any later bar trades inside its range.
     *
     * Only bars after the third bar of the pattern are considered - the third
     * bar is what created the gap, so its own range cannot fill it.
     */
    private static boolean isFilledAfter(List<Bar> bars, int middleIndex, BigDecimal low, BigDecimal high) {
        for (int j = middleIndex + 2; j < bars.size(); j++) {
            Bar later = bars.get(j);
            boolean tradedInside = later.low().compareTo(high) < 0
                    && later.high().compareTo(low) > 0;
            if (tradedInside) {
                return true;
            }
        }
        return false;
    }
}
