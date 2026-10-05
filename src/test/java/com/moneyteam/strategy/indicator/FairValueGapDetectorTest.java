package com.moneyteam.strategy.indicator;

import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.strategy.model.Direction;
import com.moneyteam.strategy.model.FairValueGap;
import com.moneyteam.strategy.model.GapSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fair Value Gap detection, specified by hand-built three-bar fixtures.
 *
 * The cases that matter most are the ones that must NOT fire. A detector that
 * finds every real gap but also fires on bars that merely touch is worse than
 * useless - it produces constant signal on ordinary price action.
 */
class FairValueGapDetectorTest {

    private static final Instant T0 = Instant.parse("2024-03-04T14:30:00Z");

    /** Bar builder: index positions the bar in time, oldest-first. */
    private static Bar bar(int index, String low, String high) {
        BigDecimal l = new BigDecimal(low);
        BigDecimal h = new BigDecimal(high);
        BigDecimal mid = l.add(h).divide(BigDecimal.valueOf(2), 4, java.math.RoundingMode.HALF_UP);
        return new Bar("TEST", T0.plus(index * 5L, ChronoUnit.MINUTES), Timeframe.M5,
                mid, h, l, mid, 1_000L);
    }

    @Test
    @DisplayName("a bullish gap is found where bar 1's high is below bar 3's low")
    void bullishGap() {
        // 10-11 | 12-15 (displacement) | 13-16  ->  11 to 13 went untraded
        List<Bar> bars = List.of(
                bar(0, "10", "11"),
                bar(1, "12", "15"),
                bar(2, "13", "16"));

        List<FairValueGap> gaps = FairValueGapDetector.detect(bars, GapSource.UNDERLYING);

        assertThat(gaps).hasSize(1);
        FairValueGap gap = gaps.get(0);
        assertThat(gap.direction()).isEqualTo(Direction.BULLISH);
        assertThat(gap.gapLow()).isEqualByComparingTo("11");
        assertThat(gap.gapHigh()).isEqualByComparingTo("13");
        assertThat(gap.filled()).isFalse();
    }

    @Test
    @DisplayName("a bearish gap is found where bar 1's low is above bar 3's high")
    void bearishGap() {
        // 20-22 | 15-18 (displacement) | 14-17  ->  17 to 20 went untraded
        List<Bar> bars = List.of(
                bar(0, "20", "22"),
                bar(1, "15", "18"),
                bar(2, "14", "17"));

        List<FairValueGap> gaps = FairValueGapDetector.detect(bars, GapSource.UNDERLYING);

        assertThat(gaps).hasSize(1);
        assertThat(gaps.get(0).direction()).isEqualTo(Direction.BEARISH);
        assertThat(gaps.get(0).gapLow()).isEqualByComparingTo("17");
        assertThat(gaps.get(0).gapHigh()).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("bars that exactly touch are NOT a gap - nothing went untraded")
    void touchingIsNotAGap() {
        // bar 1 high == bar 3 low. They meet; no range was skipped.
        List<Bar> bars = List.of(
                bar(0, "10", "12"),
                bar(1, "11", "15"),
                bar(2, "12", "16"));

        assertThat(FairValueGapDetector.detect(bars, GapSource.UNDERLYING)).isEmpty();
    }

    @Test
    @DisplayName("overlapping bars are not a gap")
    void overlapIsNotAGap() {
        List<Bar> bars = List.of(
                bar(0, "10", "13"),
                bar(1, "11", "15"),
                bar(2, "12", "16"));

        assertThat(FairValueGapDetector.detect(bars, GapSource.UNDERLYING)).isEmpty();
    }

    @Test
    @DisplayName("fewer than three bars cannot form a gap")
    void tooFewBars() {
        assertThat(FairValueGapDetector.detect(List.of(), GapSource.UNDERLYING)).isEmpty();
        assertThat(FairValueGapDetector.detect(List.of(bar(0, "10", "11")), GapSource.UNDERLYING)).isEmpty();
        assertThat(FairValueGapDetector.detect(
                List.of(bar(0, "10", "11"), bar(1, "13", "15")), GapSource.UNDERLYING)).isEmpty();
    }

    @Test
    @DisplayName("a gap later traded back through is marked filled")
    void gapIsMarkedFilledWhenPriceReturns() {
        List<Bar> bars = List.of(
                bar(0, "10", "11"),
                bar(1, "12", "15"),
                bar(2, "13", "16"),
                bar(3, "11", "14"));   // trades back down into the 11-13 gap

        List<FairValueGap> gaps = FairValueGapDetector.detect(bars, GapSource.UNDERLYING);

        assertThat(gaps).hasSize(1);
        assertThat(gaps.get(0).filled()).isTrue();
        assertThat(FairValueGapDetector.detectUnfilled(bars, GapSource.UNDERLYING)).isEmpty();
    }

    @Test
    @DisplayName("a gap price has not returned to stays unfilled")
    void gapStaysUnfilledWhenPriceRunsAway() {
        List<Bar> bars = List.of(
                bar(0, "10", "11"),
                bar(1, "12", "15"),
                bar(2, "13", "16"),
                bar(3, "15", "18"));   // keeps going up, never revisits 11-13

        assertThat(FairValueGapDetector.detectUnfilled(bars, GapSource.UNDERLYING)).hasSize(1);
    }

    @Test
    @DisplayName("the third bar of the pattern cannot fill the gap it just created")
    void creatingBarDoesNotFillItsOwnGap() {
        // The displacement bar's own range overlaps the gap by construction;
        // counting it would mean no gap is ever unfilled.
        List<Bar> bars = List.of(
                bar(0, "10", "11"),
                bar(1, "12", "15"),
                bar(2, "13", "16"));

        assertThat(FairValueGapDetector.detect(bars, GapSource.UNDERLYING).get(0).filled()).isFalse();
    }

    @Test
    @DisplayName("several gaps in one series are all returned, in formation order")
    void multipleGaps() {
        // Every rolling three-bar window is a candidate, so the middle window
        // (bars 1-3) is deliberately made to overlap: 15 is not below 14, so it
        // forms no gap and the series yields exactly two.
        List<Bar> bars = List.of(
                bar(0, "10", "11"),
                bar(1, "12", "15"),
                bar(2, "13", "16"),   // window 0-2 -> gap 11 to 13
                bar(3, "14", "17"),   // window 1-3 -> overlaps, no gap
                bar(4, "18", "21"));  // window 2-4 -> gap 16 to 18

        List<FairValueGap> gaps = FairValueGapDetector.detect(bars, GapSource.UNDERLYING);

        assertThat(gaps).hasSize(2);
        assertThat(gaps.get(0).gapLow()).isEqualByComparingTo("11");
        assertThat(gaps.get(0).gapHigh()).isEqualByComparingTo("13");
        assertThat(gaps.get(1).gapLow()).isEqualByComparingTo("16");
        assertThat(gaps.get(1).gapHigh()).isEqualByComparingTo("18");
        assertThat(gaps.get(0).formedAt()).isBefore(gaps.get(1).formedAt());
    }

    @Test
    @DisplayName("every rolling three-bar window is examined, not just consecutive triples")
    void scansOverlappingWindows() {
        // A sustained run gaps on each successive window; all are reported.
        List<Bar> bars = List.of(
                bar(0, "10", "11"),
                bar(1, "12", "13"),
                bar(2, "14", "15"),
                bar(3, "16", "17"));

        assertThat(FairValueGapDetector.detect(bars, GapSource.UNDERLYING)).hasSize(2);
    }

    @Test
    @DisplayName("the gap records which chart it came from")
    void sourceIsRecorded() {
        List<Bar> bars = List.of(
                bar(0, "10", "11"),
                bar(1, "12", "15"),
                bar(2, "13", "16"));

        assertThat(FairValueGapDetector.detect(bars, GapSource.OPTION_CONTRACT).get(0).source())
                .isEqualTo(GapSource.OPTION_CONTRACT);
    }

    @Test
    @DisplayName("newest-first bars are rejected rather than silently inverted")
    void rejectsWrongOrdering() {
        List<Bar> reversed = new ArrayList<>(List.of(
                bar(2, "13", "16"),
                bar(1, "12", "15"),
                bar(0, "10", "11")));

        assertThatThrownBy(() -> FairValueGapDetector.detect(reversed, GapSource.UNDERLYING))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oldest-first");
    }

    @Test
    @DisplayName("a gap of zero width cannot be constructed at all")
    void zeroWidthGapIsUnrepresentable() {
        assertThatThrownBy(() -> new FairValueGap(
                Timeframe.M5, Direction.BULLISH, GapSource.UNDERLYING,
                new BigDecimal("10"), new BigDecimal("10"), T0, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must have width");
    }
}
