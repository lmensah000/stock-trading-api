package com.moneyteam.marketdata.aggregate;

import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.Timeframe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * One-hour bars built from 30-minute bars, because Schwab has no hourly
 * frequency and the Fair Value Gap rule confirms on 5m, 15m and 1h.
 */
class BarAggregatorTest {

    /** 09:30 ET on a Monday, the session open. */
    private static final Instant OPEN = Instant.parse("2024-03-04T14:30:00Z");

    private static Bar bar(int index, String open, String high, String low, String close, long volume) {
        return new Bar("AAPL", OPEN.plus(Duration.ofMinutes(30L * index)), Timeframe.M15,
                new BigDecimal(open), new BigDecimal(high),
                new BigDecimal(low), new BigDecimal(close), volume);
    }

    @Test
    @DisplayName("two 30-minute bars become one hour with the right OHLCV")
    void pairsBecomeHours() {
        List<Bar> hourly = BarAggregator.toHourly(List.of(
                bar(0, "100", "104", "99", "103", 1_000),
                bar(1, "103", "107", "102", "105", 2_000)));

        assertThat(hourly).hasSize(1);
        Bar hour = hourly.get(0);
        assertThat(hour.open()).isEqualByComparingTo("100");    // first bar's open
        assertThat(hour.high()).isEqualByComparingTo("107");    // highest of the pair
        assertThat(hour.low()).isEqualByComparingTo("99");      // lowest of the pair
        assertThat(hour.close()).isEqualByComparingTo("105");   // last bar's close
        assertThat(hour.volume()).isEqualTo(3_000);             // summed
        assertThat(hour.timeframe()).isEqualTo(Timeframe.H1);
    }

    @Test
    @DisplayName("the hour opens when its first 30-minute bar opened, aligning to the session")
    void opensAtSessionAlignedBoundary() {
        List<Bar> hourly = BarAggregator.toHourly(List.of(
                bar(0, "100", "101", "99", "100", 10),
                bar(1, "100", "101", "99", "100", 10),
                bar(2, "100", "101", "99", "100", 10),
                bar(3, "100", "101", "99", "100", 10)));

        // 09:30-10:30 and 10:30-11:30, not clock-aligned 10:00 and 11:00.
        assertThat(hourly.get(0).openTime()).isEqualTo(OPEN);
        assertThat(hourly.get(1).openTime()).isEqualTo(OPEN.plus(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("an odd trailing bar is emitted as a partial hour, not dropped")
    void trailingPartialIsKept() {
        // Three 30-minute bars: one full hour plus a half-formed one. The
        // forming bar is the one being traded, so hiding it would hide the
        // most recent price action from every rule.
        List<Bar> hourly = BarAggregator.toHourly(List.of(
                bar(0, "100", "104", "99", "103", 1_000),
                bar(1, "103", "107", "102", "105", 2_000),
                bar(2, "105", "109", "104", "108", 1_500)));

        assertThat(hourly).hasSize(2);
        Bar partial = hourly.get(1);
        assertThat(partial.open()).isEqualByComparingTo("105");
        assertThat(partial.close()).isEqualByComparingTo("108");
        assertThat(partial.volume()).isEqualTo(1_500);
        assertThat(BarAggregator.lastGroupIsPartial(List.of(bar(0,"1","1","1","1",1),
                bar(1,"1","1","1","1",1), bar(2,"1","1","1","1",1)), 2)).isTrue();
    }

    @Test
    @DisplayName("aggregated bars remain oldest-first")
    void preservesOrdering() {
        List<Bar> hourly = BarAggregator.toHourly(List.of(
                bar(0, "100", "101", "99", "100", 10),
                bar(1, "100", "101", "99", "100", 10),
                bar(2, "100", "101", "99", "100", 10),
                bar(3, "100", "101", "99", "100", 10)));

        // Throws if the invariant is broken.
        Bar.requireOldestFirst(hourly);
        assertThat(hourly.get(0).openTime()).isBefore(hourly.get(1).openTime());
    }

    @Test
    @DisplayName("an empty series aggregates to nothing rather than failing")
    void emptyInput() {
        assertThat(BarAggregator.toHourly(List.of())).isEmpty();
    }

    @Test
    @DisplayName("a single bar yields a single partial hour")
    void singleBar() {
        assertThat(BarAggregator.toHourly(List.of(bar(0, "100", "104", "99", "103", 1_000))))
                .hasSize(1);
    }

    @Test
    @DisplayName("spacing can be verified, so a mislabelled series is detectable")
    void spacingCheck() {
        List<Bar> thirtyMinute = List.of(
                bar(0, "100", "101", "99", "100", 10),
                bar(1, "100", "101", "99", "100", 10));

        assertThat(BarAggregator.spacingMatches(thirtyMinute, Duration.ofMinutes(30))).isTrue();
        assertThat(BarAggregator.spacingMatches(thirtyMinute, Duration.ofMinutes(15))).isFalse();
    }
}
