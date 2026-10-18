package com.moneyteam.marketdata.provider.csv;

import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.OptionQuote;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.marketdata.service.MarketDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Bars read from CSV files on disk.
 *
 * This exists so backtesting is possible today. The Schwab adapter cannot be
 * used until the compromised credentials are rotated, and a strategy whose
 * thresholds are untested guesses is the larger problem - so the data source
 * that needs no credentials at all comes first.
 *
 * <h2>File layout</h2>
 * One file per ticker and timeframe, named {@code TICKER_TIMEFRAME.csv}:
 * <pre>
 *   data/AAPL_D1.csv
 *   openTime,open,high,low,close,volume
 *   2026-01-02T00:00:00Z,100.00,101.00,99.00,100.00,1000
 * </pre>
 *
 * Prices parse to {@code BigDecimal} directly from text, never via
 * {@code double}. Going through a double would introduce representation error
 * before the value was ever used, and gap detection is an exact inequality
 * between two prices.
 *
 * <h2>Ordering is validated, not fixed</h2>
 * An out-of-order file is rejected through {@link Bar#requireOldestFirst}
 * rather than silently sorted. Sorting would hide the real problem - a file
 * assembled wrongly, or two sources concatenated - and every indicator depends
 * on the ordering, so a quiet repair is the kind of help that costs a wrong
 * trade later.
 *
 * <h2>What it honestly cannot do</h2>
 * {@link #getOptionChain} and {@link #getOptionBars} return empty. Equity OHLCV
 * files contain no option data, and fabricating a chain would make the
 * volume-to-open-interest rule appear tested when it was not. Empty is already
 * the documented normal answer for option bars.
 *
 * Thread-safe: files are parsed once and cached as immutable lists.
 */
public class CsvMarketDataProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(CsvMarketDataProvider.class);
    private static final String EXPECTED_HEADER = "openTime,open,high,low,close,volume";

    private final Path directory;
    private final Map<String, List<Bar>> cache = new ConcurrentHashMap<>();

    public CsvMarketDataProvider(Path directory) {
        this.directory = directory;
    }

    private static String key(String ticker, Timeframe timeframe) {
        return ticker.toUpperCase() + "_" + timeframe.name();
    }

    @Override
    public List<Bar> getBars(String ticker, Timeframe timeframe, int lookback) {
        List<Bar> all = cache.computeIfAbsent(key(ticker, timeframe), k -> load(ticker, timeframe));
        if (all.size() <= lookback) {
            return all;
        }
        // Most recent `lookback` bars, still oldest-first.
        return List.copyOf(all.subList(all.size() - lookback, all.size()));
    }

    /** Every bar on file, oldest-first. Used by the backtest, which replays all of them. */
    public List<Bar> getAllBars(String ticker, Timeframe timeframe) {
        return cache.computeIfAbsent(key(ticker, timeframe), k -> load(ticker, timeframe));
    }

    @Override
    public List<OptionQuote> getOptionChain(String ticker) {
        return List.of();
    }

    @Override
    public List<Bar> getOptionBars(String contractKey, Timeframe timeframe, int lookback) {
        return List.of();
    }

    /**
     * The close before the most recent bar on file.
     *
     * <strong>Not safe for a backtest to call.</strong> "Most recent on file" is
     * a future price at any replay step before the end, so
     * {@link com.moneyteam.bot.backtest.BacktestRunner} derives the previous
     * close from its own truncated window instead. This method serves live-style
     * use, where the end of the file is genuinely the present.
     */
    @Override
    public BigDecimal getPreviousClose(String ticker) {
        List<Bar> daily = getAllBars(ticker, Timeframe.D1);
        return daily.size() < 2 ? null : daily.get(daily.size() - 2).close();
    }

    private List<Bar> load(String ticker, Timeframe timeframe) {
        Path file = directory.resolve(key(ticker, timeframe) + ".csv");
        if (!Files.isRegularFile(file)) {
            log.debug("No CSV for {} {} at {}", ticker, timeframe, file);
            return List.of();
        }

        List<Bar> bars = new ArrayList<>();
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            int[] lineNumber = {0};
            lines.forEach(line -> {
                lineNumber[0]++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    return;
                }
                if (lineNumber[0] == 1 && trimmed.replace(" ", "").equalsIgnoreCase(EXPECTED_HEADER)) {
                    return;
                }
                bars.add(parse(ticker, timeframe, trimmed, file, lineNumber[0]));
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }

        // Rejected rather than sorted. See the class note.
        Bar.requireOldestFirst(bars);

        log.info("Loaded {} {} bars for {} from {}", bars.size(), timeframe, ticker, file.getFileName());
        return List.copyOf(bars);
    }

    private static Bar parse(String ticker, Timeframe timeframe, String line, Path file, int lineNumber) {
        String[] parts = line.split(",", -1);
        if (parts.length < 6) {
            throw new IllegalArgumentException(
                    "Expected 6 columns (%s) at %s line %d, got %d"
                            .formatted(EXPECTED_HEADER, file.getFileName(), lineNumber, parts.length));
        }
        try {
            return new Bar(
                    ticker.toUpperCase(),
                    Instant.parse(parts[0].trim()),
                    timeframe,
                    new BigDecimal(parts[1].trim()),
                    new BigDecimal(parts[2].trim()),
                    new BigDecimal(parts[3].trim()),
                    new BigDecimal(parts[4].trim()),
                    Long.parseLong(parts[5].trim()));
        } catch (RuntimeException e) {
            // Names the file and line: a malformed row in a thousand-line file
            // is otherwise genuinely hard to find.
            throw new IllegalArgumentException(
                    "Malformed bar at %s line %d: %s".formatted(file.getFileName(), lineNumber, e.getMessage()), e);
        }
    }
}
