package com.moneyteam.marketdata.provider.alpaca;

import com.fasterxml.jackson.databind.JsonNode;
import com.moneyteam.bot.broker.alpaca.AlpacaApiException;
import com.moneyteam.bot.broker.alpaca.AlpacaHttpClient;
import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.OptionQuote;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.marketdata.service.MarketDataProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Bars from Alpaca's market-data API.
 *
 * <h2>Two limits of the free tier that change what you may conclude</h2>
 * <ul>
 *   <li><strong>The feed is IEX, which is a small share of consolidated
 *       volume.</strong> Prices are broadly right, so a percentage-move rule is
 *       fine. <em>Volume is not.</em> A volume threshold calibrated against IEX
 *       is calibrated against a few percent of what actually traded, so the
 *       strategy's "volume confirms" condition and
 *       {@code VolumeOpenInterestRule} cannot be validated on this feed. Either
 *       treat volume signals as unverified or subscribe to SIP.</li>
 *   <li><strong>Historical data is delayed about fifteen minutes.</strong>
 *       Harmless for daily bars and backtests; it means an intraday loop reads
 *       stale bars, so M5 and M15 should stay backtest-only here.</li>
 * </ul>
 *
 * These are stated in the log on first use rather than only in a comment,
 * because the person reading a surprising signal is looking at logs.
 *
 * <h2>No option data</h2>
 * {@link #getOptionChain} returns empty: options are a paid tier. Empty is the
 * honest answer and keeps the option rules visibly unvalidated rather than fed
 * fabricated input.
 */
public class AlpacaMarketDataProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(AlpacaMarketDataProvider.class);

    private final AlpacaHttpClient http;
    private final String feed;

    public AlpacaMarketDataProvider(AlpacaHttpClient http, String feed) {
        this.http = http;
        this.feed = feed;
        if ("iex".equalsIgnoreCase(feed)) {
            log.warn("Alpaca market data is using the IEX feed. Prices are usable, but IEX "
                    + "represents a small share of consolidated volume - do not calibrate any "
                    + "volume threshold on it, and expect historical bars to lag by ~15 minutes.");
        }
    }

    /**
     * Maps our timeframe onto Alpaca's string form.
     *
     * Note what is absent: Alpaca offers a native {@code 1Hour} bar, so unlike
     * Schwab there is nothing to aggregate. {@code BarAggregator}'s 30-minute
     * pairing exists only because Schwab has no hourly frequency.
     */
    static String alpacaTimeframe(Timeframe timeframe) {
        return switch (timeframe) {
            case M5 -> "5Min";
            case M15 -> "15Min";
            case H1 -> "1Hour";
            case D1 -> "1Day";
            case W1 -> "1Week";
        };
    }

    @Override
    @RateLimiter(name = "alpacaMarketData")
    @Retry(name = "alpacaMarketData")
    @CircuitBreaker(name = "alpacaMarketData")
    public List<Bar> getBars(String ticker, Timeframe timeframe, int lookback) {
        String symbol = ticker.toUpperCase();
        String query = "timeframe=%s&limit=%d&feed=%s&adjustment=split&sort=asc"
                .formatted(alpacaTimeframe(timeframe), Math.max(1, lookback), feed);

        JsonNode response = http.get("/v2/stocks/" + symbol + "/bars", query);
        JsonNode bars = response.path("bars");
        if (bars.isMissingNode() || !bars.isArray()) {
            return List.of();
        }

        List<Bar> series = new ArrayList<>(bars.size());
        for (JsonNode node : bars) {
            series.add(toBar(symbol, timeframe, node));
        }

        // asc was requested, but the guard stays: a provider silently changing
        // its ordering would otherwise invert every indicator rather than fail.
        Bar.requireOldestFirst(series);
        return List.copyOf(series);
    }

    @Override
    public List<OptionQuote> getOptionChain(String ticker) {
        return List.of();
    }

    @Override
    public List<Bar> getOptionBars(String contractKey, Timeframe timeframe, int lookback) {
        return List.of();
    }

    @Override
    @RateLimiter(name = "alpacaMarketData")
    @Retry(name = "alpacaMarketData")
    @CircuitBreaker(name = "alpacaMarketData")
    public BigDecimal getPreviousClose(String ticker) {
        // Two daily bars, so the close before the most recent one is available.
        List<Bar> daily = getBars(ticker, Timeframe.D1, 2);
        return daily.size() < 2 ? null : daily.get(daily.size() - 2).close();
    }

    private static Bar toBar(String symbol, Timeframe timeframe, JsonNode node) {
        return new Bar(
                symbol,
                Instant.parse(node.path("t").asText()),
                timeframe,
                // Alpaca sends bar prices as JSON numbers. decimalValue()
                // preserves the exact decimal; asDouble() would introduce
                // representation error before the value was ever compared, and
                // gap detection is an exact inequality.
                node.path("o").decimalValue(),
                node.path("h").decimalValue(),
                node.path("l").decimalValue(),
                node.path("c").decimalValue(),
                node.path("v").asLong());
    }

    /** True when the failure means the key pair does not match the host. */
    public static boolean isCredentialHostMismatch(RuntimeException e) {
        return e instanceof AlpacaApiException alpaca && alpaca.isCredentialHostMismatch();
    }
}
