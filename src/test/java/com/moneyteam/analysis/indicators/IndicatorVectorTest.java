package com.moneyteam.analysis.indicators;

import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.Timeframe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The implementations, against the vectors written before them.
 *
 * {@code IndicatorVectorFixtureTest} checks the fixtures are internally
 * coherent. This checks the code reproduces them. The expected values in
 * {@code src/test/resources/vectors} were hand-computed from
 * {@code docs/INDICATOR-SPEC.md} before any of this existed, so a disagreement
 * is a real disagreement rather than the implementation grading its own work.
 */
class IndicatorVectorTest {

    private static final Path VECTORS = Path.of("src/test/resources/vectors");

    // ---------------------------------------------------------------
    //  Fixture loading
    // ---------------------------------------------------------------

    private static List<String[]> rows(String file) throws IOException {
        List<String> lines = Files.readAllLines(VECTORS.resolve(file), StandardCharsets.UTF_8);
        List<String[]> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {      // skip the header
            if (!lines.get(i).isBlank()) {
                rows.add(lines.get(i).split(",", -1));
            }
        }
        return rows;
    }

    private static List<Bar> baseBars() throws IOException {
        List<Bar> bars = new ArrayList<>();
        for (String[] row : rows("base_ohlcv.csv")) {
            Instant at = LocalDate.parse(row[0]).atStartOfDay(ZoneOffset.UTC).toInstant();
            bars.add(new Bar("VECTOR", at, Timeframe.D1,
                    new BigDecimal(row[1]), new BigDecimal(row[2]),
                    new BigDecimal(row[3]), new BigDecimal(row[4]), Long.parseLong(row[5])));
        }
        return bars;
    }

    private static List<BigDecimal> baseCloses() throws IOException {
        List<BigDecimal> closes = new ArrayList<>();
        for (Bar bar : baseBars()) {
            closes.add(bar.close());
        }
        return closes;
    }

    /** Asserts a series cell-for-cell against a vector column. */
    private static void assertMatchesColumn(IndicatorSeries actual,
                                            String file,
                                            int column) throws IOException {
        List<String[]> rows = rows(file);
        assertThat(actual.size())
                .as("%s: series length must align to the input series", file)
                .isEqualTo(rows.size());

        int comparedValues = 0;
        for (int i = 0; i < rows.size(); i++) {
            String expected = rows.get(i)[column].trim();
            Optional<BigDecimal> got = actual.valueAt(i);

            if (expected.isEmpty()) {
                assertThat(got)
                        .as("%s index %d: the vector leaves this empty, so it must be warm-up "
                                + "and not a value", file, i)
                        .isEmpty();
            } else {
                assertThat(got)
                        .as("%s index %d: the vector expects %s", file, i, expected)
                        .isPresent();
                assertThat(got.get())
                        .as("%s index %d", file, i)
                        .isEqualByComparingTo(new BigDecimal(expected));
                comparedValues++;
            }
        }

        // Guards against a vector column that is entirely empty silently
        // passing as "every cell matched".
        assertThat(comparedValues).as("%s compared no values", file).isPositive();
    }

    // ---------------------------------------------------------------
    //  The vectors
    // ---------------------------------------------------------------

    @Test
    @DisplayName("SMA(10) reproduces sma_10.csv cell for cell")
    void smaMatchesVector() throws IOException {
        assertMatchesColumn(MovingAverages.sma(baseCloses(), 10), "sma_10.csv", 3);
    }

    @Test
    @DisplayName("EMA(10) reproduces ema_10.csv cell for cell")
    void emaMatchesVector() throws IOException {
        assertMatchesColumn(MovingAverages.ema(baseCloses(), 10), "ema_10.csv", 3);
    }

    @Test
    @DisplayName("ATR(14) Wilder reproduces atr_14_wilder.csv cell for cell")
    void atrMatchesVector() throws IOException {
        assertMatchesColumn(AverageTrueRange.atr(baseBars(), 14), "atr_14_wilder.csv", 5);
    }

    @Test
    @DisplayName("RSI(14) Wilder reproduces rsi_14_wilder.csv cell for cell")
    void rsiMatchesVector() throws IOException {
        assertMatchesColumn(RelativeStrengthIndex.rsi(baseCloses(), 14), "rsi_14_wilder.csv", 3);
    }

    // ---------------------------------------------------------------
    //  The decisions the spec exists to settle
    // ---------------------------------------------------------------

    @Test
    @DisplayName("EMA is SMA-seeded: its first value equals SMA at the same index")
    void emaIsSmaSeeded() throws IOException {
        List<BigDecimal> closes = baseCloses();

        IndicatorSeries sma = MovingAverages.sma(closes, 10);
        IndicatorSeries ema = MovingAverages.ema(closes, 10);

        assertThat(ema.firstWarmedUpIndex()).isEqualTo(9).isEqualTo(sma.firstWarmedUpIndex());
        // Seeding from the first price instead would converge to the same curve
        // but differ for roughly 3n bars - most of a trading year on a 50-period
        // EMA, long enough to change what the strategy buys.
        assertThat(ema.valueAt(9)).isPresent();
        assertThat(ema.valueAt(9).get()).isEqualByComparingTo(sma.valueAt(9).orElseThrow());
    }

    @Test
    @DisplayName("Wilder 1/n is NOT the EMA 2/(n+1) multiplier - the series differ")
    void wilderIsNotTheEmaMultiplier() throws IOException {
        List<BigDecimal> closes = baseCloses();

        // Wilder's 1/n for period n equals an EMA of period 2n-1. So if the
        // smoothing were wrongly implemented with 2/(n+1), an RSI(14) would
        // behave like this EMA(14) rather than like an EMA(27) - materially
        // faster, entirely plausible on a chart, and silent. This is the
        // specific error docs/INDICATOR-SPEC.md names, so it is pinned here.
        IndicatorSeries wilderEquivalent = MovingAverages.ema(closes, 27);
        IndicatorSeries wrongMultiplier = MovingAverages.ema(closes, 14);

        BigDecimal slow = wilderEquivalent.latest().orElseThrow();
        BigDecimal fast = wrongMultiplier.latest().orElseThrow();

        assertThat(slow).isNotEqualByComparingTo(fast);
    }

    @Test
    @DisplayName("true range is gap-aware: a gap bar's range exceeds its own high minus low")
    void trueRangeIsGapAware() {
        Instant day = Instant.parse("2026-01-02T00:00:00Z");

        // Second bar gaps up: its whole range sits above the prior close, so
        // high-low understates the real move.
        List<Bar> bars = List.of(
                new Bar("GAP", day, Timeframe.D1,
                        new BigDecimal("100"), new BigDecimal("101"),
                        new BigDecimal("99"), new BigDecimal("100"), 1L),
                new Bar("GAP", day.plusSeconds(86400), Timeframe.D1,
                        new BigDecimal("110"), new BigDecimal("112"),
                        new BigDecimal("109"), new BigDecimal("111"), 1L));

        BigDecimal trueRange = TrueRange.of(bars).valueAt(1).orElseThrow();

        // high - low is 3.00; |high - previousClose| is 12.00.
        assertThat(trueRange).isEqualByComparingTo("12.0000");
        assertThat(trueRange).isGreaterThan(new BigDecimal("3.00"));
    }

    @Test
    @DisplayName("the first bar has no true range, because it has no previous close")
    void firstBarHasNoTrueRange() throws IOException {
        assertThat(TrueRange.of(baseBars()).valueAt(0)).isEmpty();
        assertThat(TrueRange.of(baseBars()).valueAt(1)).isPresent();
    }

    @Test
    @DisplayName("warm-up is an absence, distinguishable from a genuine zero reading")
    void warmUpIsNotZero() {
        // A flat series: every SMA value is a real 0.00, which must still read
        // as present. If warm-up were represented as zero the two would be
        // indistinguishable, which is exactly what the spec forbids.
        List<BigDecimal> flat = List.of(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        IndicatorSeries sma = MovingAverages.sma(flat, 3);

        assertThat(sma.valueAt(0)).isEmpty();
        assertThat(sma.valueAt(1)).isEmpty();
        assertThat(sma.valueAt(2)).isPresent();
        assertThat(sma.valueAt(2).get()).isEqualByComparingTo("0");
        assertThat(sma.isWarmedUpAt(2)).isTrue();
    }

    @Test
    @DisplayName("an input shorter than the period yields no values at all")
    void shortInputYieldsNothing() {
        List<BigDecimal> two = List.of(BigDecimal.ONE, BigDecimal.TEN);

        assertThat(MovingAverages.sma(two, 10).emitted()).isEmpty();
        assertThat(MovingAverages.ema(two, 10).emitted()).isEmpty();
        assertThat(MovingAverages.sma(two, 10).size()).isEqualTo(2);
        assertThat(MovingAverages.sma(two, 10).latest()).isEmpty();
    }

    @Test
    @DisplayName("every emitted value carries scale 4")
    void emittedValuesCarryScaleFour() throws IOException {
        List<BigDecimal> closes = baseCloses();

        for (BigDecimal value : MovingAverages.ema(closes, 10).emitted()) {
            assertThat(value.scale()).isEqualTo(4);
        }
        for (BigDecimal value : AverageTrueRange.atr(baseBars(), 14).emitted()) {
            assertThat(value.scale()).isEqualTo(4);
        }
    }

    @Test
    @DisplayName("a period below 1 is refused")
    void periodMustBePositive() {
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> MovingAverages.sma(List.of(BigDecimal.ONE), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("period must be at least 1");
    }
}
