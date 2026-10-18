package com.moneyteam.marketdata.provider.csv;

import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.Timeframe;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reading bars from disk.
 *
 * The behaviours worth protecting are the refusals: an out-of-order file is
 * rejected rather than sorted, and option data is reported as absent rather
 * than fabricated. Both are places where being helpful would be the bug.
 */
class CsvMarketDataProviderTest {

    private static final String HEADER = "openTime,open,high,low,close,volume\n";

    private static Path write(Path dir, String name, String body) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, body);
        return file;
    }

    @Test
    @DisplayName("bars load oldest-first with BigDecimal prices")
    void loadsBars(@TempDir Path dir) throws IOException {
        write(dir, "TEST_D1.csv", HEADER
                + "2026-01-02T00:00:00Z,100.00,101.00,99.00,100.00,1000\n"
                + "2026-01-05T00:00:00Z,100.00,106.00,99.50,105.00,5000\n");

        List<Bar> bars = new CsvMarketDataProvider(dir).getAllBars("TEST", Timeframe.D1);

        assertThat(bars).hasSize(2);
        assertThat(bars.get(0).openTime()).isEqualTo(Instant.parse("2026-01-02T00:00:00Z"));
        assertThat(bars.get(1).close()).isEqualByComparingTo("105.00");
        assertThat(bars.get(1).volume()).isEqualTo(5000L);
        // Index 0 is earliest - the ordering every indicator assumes.
        assertThat(bars.get(0).openTime()).isBefore(bars.get(1).openTime());
    }

    @Test
    @DisplayName("prices keep their exact decimal value, never via double")
    void pricesAreExact(@TempDir Path dir) throws IOException {
        // 0.1 + 0.2 territory: a value that cannot be represented exactly in
        // binary floating point. Gap detection is an exact inequality, so a
        // parse through double would make the comparison depend on rounding.
        write(dir, "TEST_D1.csv", HEADER
                + "2026-01-02T00:00:00Z,0.30,0.30,0.10,0.30,1\n");

        Bar bar = new CsvMarketDataProvider(dir).getAllBars("TEST", Timeframe.D1).get(0);

        assertThat(bar.close()).isEqualByComparingTo(new java.math.BigDecimal("0.30"));
        assertThat(bar.close().toPlainString()).isEqualTo("0.30");
    }

    @Test
    @DisplayName("an out-of-order file is rejected, not silently sorted")
    void rejectsOutOfOrderRows(@TempDir Path dir) throws IOException {
        write(dir, "TEST_D1.csv", HEADER
                + "2026-01-05T00:00:00Z,100.00,106.00,99.50,105.00,5000\n"
                + "2026-01-02T00:00:00Z,100.00,101.00,99.00,100.00,1000\n");

        // Sorting would hide the real cause - a file assembled wrongly, or two
        // sources concatenated - and the quiet repair costs a wrong trade later.
        assertThatThrownBy(() -> new CsvMarketDataProvider(dir).getAllBars("TEST", Timeframe.D1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oldest-first");
    }

    @Test
    @DisplayName("a malformed row names the file and line")
    void malformedRowIsLocatable(@TempDir Path dir) throws IOException {
        write(dir, "TEST_D1.csv", HEADER
                + "2026-01-02T00:00:00Z,100.00,101.00,99.00,100.00,1000\n"
                + "2026-01-05T00:00:00Z,not-a-price,106.00,99.50,105.00,5000\n");

        assertThatThrownBy(() -> new CsvMarketDataProvider(dir).getAllBars("TEST", Timeframe.D1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TEST_D1.csv")
                .hasMessageContaining("line 3");
    }

    @Test
    @DisplayName("a row with too few columns is rejected")
    void rejectsShortRow(@TempDir Path dir) throws IOException {
        write(dir, "TEST_D1.csv", HEADER + "2026-01-02T00:00:00Z,100.00,101.00\n");

        assertThatThrownBy(() -> new CsvMarketDataProvider(dir).getAllBars("TEST", Timeframe.D1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("6 columns");
    }

    @Test
    @DisplayName("a bar whose high is below its low is refused by Bar's own guard")
    void rejectsImpossibleBar(@TempDir Path dir) throws IOException {
        write(dir, "TEST_D1.csv", HEADER + "2026-01-02T00:00:00Z,100.00,98.00,99.00,100.00,1000\n");

        assertThatThrownBy(() -> new CsvMarketDataProvider(dir).getAllBars("TEST", Timeframe.D1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("blank lines and comments are skipped")
    void skipsBlanksAndComments(@TempDir Path dir) throws IOException {
        write(dir, "TEST_D1.csv", HEADER
                + "# exported 2026-01-08\n"
                + "\n"
                + "2026-01-02T00:00:00Z,100.00,101.00,99.00,100.00,1000\n");

        assertThat(new CsvMarketDataProvider(dir).getAllBars("TEST", Timeframe.D1)).hasSize(1);
    }

    @Test
    @DisplayName("a missing file is an empty series, not an error")
    void missingFileIsEmpty(@TempDir Path dir) {
        // A universe scan should not fail because one ticker has no file yet.
        assertThat(new CsvMarketDataProvider(dir).getAllBars("NOPE", Timeframe.D1)).isEmpty();
    }

    @Test
    @DisplayName("lookback returns the most recent bars, still oldest-first")
    void lookbackTakesTheMostRecent(@TempDir Path dir) throws IOException {
        write(dir, "TEST_D1.csv", HEADER
                + "2026-01-02T00:00:00Z,1.00,1.00,1.00,1.00,1\n"
                + "2026-01-05T00:00:00Z,2.00,2.00,2.00,2.00,1\n"
                + "2026-01-06T00:00:00Z,3.00,3.00,3.00,3.00,1\n");

        List<Bar> bars = new CsvMarketDataProvider(dir).getBars("TEST", Timeframe.D1, 2);

        assertThat(bars).hasSize(2);
        assertThat(bars.get(0).close()).isEqualByComparingTo("2.00");
        assertThat(bars.get(1).close()).isEqualByComparingTo("3.00");
    }

    @Test
    @DisplayName("option data is reported as absent rather than fabricated")
    void optionDataIsEmpty(@TempDir Path dir) {
        CsvMarketDataProvider provider = new CsvMarketDataProvider(dir);

        // Equity OHLCV files contain no option data. Inventing a chain would
        // make the volume-to-open-interest rule look tested when it is not.
        assertThat(provider.getOptionChain("TEST")).isEmpty();
        assertThat(provider.getOptionBars("TEST_C100", Timeframe.M5, 10)).isEmpty();
    }

    @Test
    @DisplayName("previousClose is the close before the last bar on file")
    void previousCloseIsSecondToLast(@TempDir Path dir) throws IOException {
        write(dir, "TEST_D1.csv", HEADER
                + "2026-01-02T00:00:00Z,1.00,1.00,1.00,1.00,1\n"
                + "2026-01-05T00:00:00Z,2.00,2.00,2.00,2.00,1\n"
                + "2026-01-06T00:00:00Z,3.00,3.00,3.00,3.00,1\n");

        assertThat(new CsvMarketDataProvider(dir).getPreviousClose("TEST"))
                .isEqualByComparingTo("2.00");
    }

    @Test
    @DisplayName("a single-bar file has no previous close")
    void singleBarHasNoPreviousClose(@TempDir Path dir) throws IOException {
        write(dir, "TEST_D1.csv", HEADER + "2026-01-02T00:00:00Z,1.00,1.00,1.00,1.00,1\n");

        assertThat(new CsvMarketDataProvider(dir).getPreviousClose("TEST")).isNull();
    }

    @Test
    @DisplayName("the committed spec fixture parses and matches the documented values")
    void specFixtureParses() {
        // Guards docs/BACKTEST-SPEC.md against drifting from the fixture the
        // runner tests rely on.
        Path resources = Path.of("src/test/resources/backtest");

        List<Bar> bars = new CsvMarketDataProvider(resources).getAllBars("TEST", Timeframe.D1);

        assertThat(bars).hasSize(4);
        assertThat(bars.get(1).close()).isEqualByComparingTo("105.00");
        assertThat(bars.get(1).low()).isEqualByComparingTo("99.50");
        assertThat(bars.get(2).open()).isEqualByComparingTo("105.50");
        assertThat(bars.get(3).high()).isEqualByComparingTo("118.00");
    }
}
