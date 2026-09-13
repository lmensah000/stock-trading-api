package com.moneyteam.analysis.indicators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the indicator reference vectors described in docs/INDICATOR-SPEC.md.
 *
 * The indicators themselves do not exist yet (Phase 6), so there is nothing to
 * assert them against. What can be asserted now is that the specification is
 * well-formed and internally consistent - so a corrupted or hand-edited fixture
 * fails here, immediately, rather than being discovered as a confusing red test
 * while someone is midway through writing an indicator.
 */
class IndicatorVectorFixtureTest {

    private static final String DIR = "vectors/";

    private static List<String[]> load(String file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (InputStream in = IndicatorVectorFixtureTest.class.getClassLoader()
                .getResourceAsStream(DIR + file)) {
            assertThat(in).as("fixture %s must exist on the test classpath", file).isNotNull();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (!line.isBlank()) {
                        rows.add(line.split(",", -1));
                    }
                }
            }
        }
        return rows;
    }

    private static final String[] ALL_FIXTURES = {
            "base_ohlcv.csv", "sma_10.csv", "ema_10.csv",
            "rsi_14_wilder.csv", "atr_14_wilder.csv",
            "weekly_rollup.csv", "contraction_ratio_5.csv"
    };

    @ParameterizedTest
    @ValueSource(strings = {
            "base_ohlcv.csv", "sma_10.csv", "ema_10.csv",
            "rsi_14_wilder.csv", "atr_14_wilder.csv",
            "weekly_rollup.csv", "contraction_ratio_5.csv"
    })
    @DisplayName("every fixture parses, has a header, and is rectangular")
    void fixtureIsRectangular(String file) throws IOException {
        List<String[]> rows = load(file);
        assertThat(rows).as("%s has a header and at least one data row", file).hasSizeGreaterThan(1);

        int width = rows.get(0).length;
        for (int i = 1; i < rows.size(); i++) {
            assertThat(rows.get(i))
                    .as("%s row %d has the same column count as the header", file, i)
                    .hasSize(width);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"sma_10.csv", "ema_10.csv", "rsi_14_wilder.csv",
            "atr_14_wilder.csv", "contraction_ratio_5.csv"})
    @DisplayName("dates run oldest-first, as the bar-ordering invariant requires")
    void datesAreAscending(String file) throws IOException {
        List<String[]> rows = load(file);
        int dateCol = indexOf(rows.get(0), "date");
        LocalDate previous = null;
        for (int i = 1; i < rows.size(); i++) {
            LocalDate d = LocalDate.parse(rows.get(i)[dateCol]);
            if (previous != null) {
                assertThat(d).as("%s row %d is later than the row before it", file, i).isAfter(previous);
            }
            previous = d;
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"sma_10.csv", "ema_10.csv", "rsi_14_wilder.csv", "atr_14_wilder.csv"})
    @DisplayName("warm-up cells are empty, then every later cell is populated - no gaps")
    void warmUpThenContinuous(String file) throws IOException {
        List<String[]> rows = load(file);
        int valueCol = rows.get(0).length - 1;

        boolean started = false;
        for (int i = 1; i < rows.size(); i++) {
            String v = rows.get(i)[valueCol].trim();
            if (v.isEmpty()) {
                assertThat(started)
                        .as("%s row %d is empty after values began - warm-up gaps are not allowed", file, i)
                        .isFalse();
            } else {
                started = true;
            }
        }
        assertThat(started).as("%s eventually produces values", file).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"sma_10.csv", "ema_10.csv", "rsi_14_wilder.csv", "atr_14_wilder.csv"})
    @DisplayName("every emitted value carries scale 4, as the precision rule requires")
    void valuesAreScaleFour(String file) throws IOException {
        List<String[]> rows = load(file);
        int valueCol = rows.get(0).length - 1;
        for (int i = 1; i < rows.size(); i++) {
            String v = rows.get(i)[valueCol].trim();
            if (!v.isEmpty()) {
                assertThat(new BigDecimal(v).scale())
                        .as("%s row %d value '%s' is at scale 4", file, i, v)
                        .isEqualTo(4);
            }
        }
    }

    @Test
    @DisplayName("SMA and EMA warm up on the same bar, and agree there - EMA is SMA-seeded")
    void emaIsSmaSeeded() throws IOException {
        List<String[]> sma = load("sma_10.csv");
        List<String[]> ema = load("ema_10.csv");

        int firstSma = firstPopulatedRow(sma);
        int firstEma = firstPopulatedRow(ema);
        assertThat(firstEma)
                .as("a SMA-seeded EMA emits its first value on the same bar as the SMA")
                .isEqualTo(firstSma);

        BigDecimal smaSeed = new BigDecimal(sma.get(firstSma)[sma.get(0).length - 1]);
        BigDecimal emaSeed = new BigDecimal(ema.get(firstEma)[ema.get(0).length - 1]);
        assertThat(emaSeed)
                .as("the EMA seed is the SMA of the first period, not the first close")
                .isEqualByComparingTo(smaSeed);
    }

    @Test
    @DisplayName("RSI stays within 0..100")
    void rsiIsBounded() throws IOException {
        List<String[]> rows = load("rsi_14_wilder.csv");
        int col = rows.get(0).length - 1;
        for (int i = 1; i < rows.size(); i++) {
            String v = rows.get(i)[col].trim();
            if (!v.isEmpty()) {
                BigDecimal rsi = new BigDecimal(v);
                assertThat(rsi).isBetween(BigDecimal.ZERO, new BigDecimal("100"));
            }
        }
    }

    @Test
    @DisplayName("ATR is never negative - it is a range")
    void atrIsNonNegative() throws IOException {
        List<String[]> rows = load("atr_14_wilder.csv");
        int col = rows.get(0).length - 1;
        for (int i = 1; i < rows.size(); i++) {
            String v = rows.get(i)[col].trim();
            if (!v.isEmpty()) {
                assertThat(new BigDecimal(v)).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            }
        }
    }

    @Test
    @DisplayName("weekly rollup keeps high >= low and accounts for every daily bar")
    void weeklyRollupIsCoherent() throws IOException {
        List<String[]> weekly = load("weekly_rollup.csv");
        List<String[]> daily = load("base_ohlcv.csv");

        int totalBars = 0;
        for (int i = 1; i < weekly.size(); i++) {
            String[] w = weekly.get(i);
            totalBars += Integer.parseInt(w[1]);
            assertThat(new BigDecimal(w[3]))
                    .as("week %s high is at least its low", w[0])
                    .isGreaterThanOrEqualTo(new BigDecimal(w[4]));
        }
        assertThat(totalBars)
                .as("every daily bar lands in exactly one week - none dropped, none double-counted")
                .isEqualTo(daily.size() - 1);
    }

    @Test
    @DisplayName("the contraction ratio fixture exercises both contraction and expansion")
    void contractionFixtureIsNotDegenerate() throws IOException {
        List<String[]> rows = load("contraction_ratio_5.csv");
        int col = rows.get(0).length - 1;

        boolean below = false, above = false;
        for (int i = 1; i < rows.size(); i++) {
            String v = rows.get(i)[col].trim();
            if (!v.isEmpty()) {
                int cmp = new BigDecimal(v).compareTo(BigDecimal.ONE);
                if (cmp < 0) below = true;
                if (cmp > 0) above = true;
            }
        }
        assertThat(below).as("fixture contains a contracting window (ratio < 1)").isTrue();
        assertThat(above).as("fixture contains an expanding window (ratio > 1)").isTrue();
    }

    @Test
    @DisplayName("every fixture shares the same base series length")
    void fixturesShareTheBaseSeries() throws IOException {
        int base = load("base_ohlcv.csv").size();
        for (String f : ALL_FIXTURES) {
            if (f.equals("weekly_rollup.csv") || f.equals("base_ohlcv.csv")) {
                continue; // weekly is aggregated, so intentionally shorter
            }
            assertThat(load(f).size())
                    .as("%s covers the same bars as base_ohlcv.csv", f)
                    .isEqualTo(base);
        }
    }

    private static int firstPopulatedRow(List<String[]> rows) {
        int col = rows.get(0).length - 1;
        for (int i = 1; i < rows.size(); i++) {
            if (!rows.get(i)[col].trim().isEmpty()) {
                return i;
            }
        }
        throw new AssertionError("fixture has no populated values");
    }

    private static int indexOf(String[] header, String name) {
        for (int i = 0; i < header.length; i++) {
            if (header[i].trim().equalsIgnoreCase(name)) {
                return i;
            }
        }
        throw new AssertionError("no '" + name + "' column in header");
    }
}
