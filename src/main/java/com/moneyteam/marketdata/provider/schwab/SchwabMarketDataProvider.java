package com.moneyteam.marketdata.provider.schwab;

import com.fasterxml.jackson.databind.JsonNode;
import com.moneyteam.marketdata.aggregate.BarAggregator;
import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.OptionQuote;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.marketdata.provider.schwab.client.SchwabHttpClient;
import com.moneyteam.marketdata.provider.schwab.mapper.SchwabResponseMapper;
import com.moneyteam.marketdata.service.MarketDataProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

/**
 * The live Schwab adapter behind {@link MarketDataProvider}.
 *
 * Nothing above this interface changes when this replaces the in-memory
 * adapter, which is the point of having built the engine against a port.
 *
 * <h2>One-hour bars are aggregated, not requested</h2>
 * Schwab's minute widths are 1, 5, 10, 15 and 30 - there is no hourly
 * frequency. The Fair Value Gap rule confirms across 5m, 15m and 1h, so an H1
 * request fetches 30-minute bars and pairs them in {@link BarAggregator}. The
 * boundary convention is documented there.
 *
 * <h2>Minute history is shallow</h2>
 * Schwab caps intraday history to roughly ten days. A request for more comes
 * back short rather than failing, so a truncated series is logged rather than
 * being mistaken for a quiet market. The exact depth needs confirming against
 * a live key; it is configuration, not a constant.
 *
 * <h2>Resilience</h2>
 * Every outbound call carries a rate limiter, retry with exponential backoff
 * and jitter, and a circuit breaker, configured in application properties so
 * the limits are tunable per environment rather than compiled in.
 */
public class SchwabMarketDataProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(SchwabMarketDataProvider.class);

    private static final String BACKEND = "schwabMarketData";
    private static final String PRICE_HISTORY = "/marketdata/v1/pricehistory";
    private static final String CHAINS = "/marketdata/v1/chains";

    private final SchwabHttpClient http;
    private final Clock clock;

    public SchwabMarketDataProvider(SchwabHttpClient http, Clock clock) {
        this.http = http;
        this.clock = clock;
    }

    @Override
    @RateLimiter(name = BACKEND)
    @Retry(name = BACKEND)
    @CircuitBreaker(name = BACKEND)
    public List<Bar> getBars(String ticker, Timeframe timeframe, int lookback) {
        if (timeframe == Timeframe.H1) {
            // Two 30-minute bars per hour, so fetch twice as many.
            List<Bar> thirtyMinute = fetchBars(ticker, Timeframe.M15, 30, lookback * 2);
            return BarAggregator.toHourly(thirtyMinute);
        }
        return switch (timeframe) {
            case M5 -> fetchBars(ticker, timeframe, 5, lookback);
            case M15 -> fetchBars(ticker, timeframe, 15, lookback);
            case D1 -> fetchDaily(ticker, lookback);
            case W1 -> fetchWeekly(ticker, lookback);
            default -> throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
        };
    }

    private List<Bar> fetchBars(String ticker, Timeframe asTimeframe, int minutes, int lookback) {
        String query = "symbol=" + ticker
                + "&periodType=day"
                + "&period=10"               // Schwab's intraday ceiling
                + "&frequencyType=minute"
                + "&frequency=" + minutes
                + "&needExtendedHoursData=false";

        JsonNode root = http.get(PRICE_HISTORY, query);
        List<Bar> bars = SchwabResponseMapper.toBars(root, ticker, asTimeframe);

        if (bars.size() < lookback) {
            // Said out loud: a short series from a shallow history looks
            // identical to a quiet market unless it is reported.
            log.debug("Schwab returned {} {} bars for {} but {} were requested; "
                            + "intraday history is capped at roughly ten days",
                    bars.size(), asTimeframe, ticker, lookback);
        }
        return tail(bars, lookback);
    }

    private List<Bar> fetchDaily(String ticker, int lookback) {
        String query = "symbol=" + ticker
                + "&periodType=year&period=1&frequencyType=daily&frequency=1";
        return tail(SchwabResponseMapper.toBars(http.get(PRICE_HISTORY, query), ticker, Timeframe.D1), lookback);
    }

    private List<Bar> fetchWeekly(String ticker, int lookback) {
        String query = "symbol=" + ticker
                + "&periodType=year&period=3&frequencyType=weekly&frequency=1";
        return tail(SchwabResponseMapper.toBars(http.get(PRICE_HISTORY, query), ticker, Timeframe.W1), lookback);
    }

    @Override
    @RateLimiter(name = BACKEND)
    @Retry(name = BACKEND)
    @CircuitBreaker(name = BACKEND)
    public List<OptionQuote> getOptionChain(String ticker) {
        JsonNode root = http.get(CHAINS, "symbol=" + ticker + "&includeUnderlyingQuote=true");

        // asOf is now; openInterestAsOf is the previous session close.
        //
        // These are genuinely different instants and the gap matters. Open
        // interest is published once daily, so an intraday volume/open-interest
        // ratio divides live volume by a figure struck at the last close.
        // Setting openInterestAsOf to now would assert the denominator is live
        // and silently disable the staleness guard in VolumeOpenInterestRule.
        var now = clock.instant();
        return SchwabResponseMapper.toOptionQuotes(
                root, ticker, now, SchwabResponseMapper.previousSessionClose(now));
    }

    /**
     * Schwab does not expose candles for individual option contracts through
     * the market-data API, so this returns empty rather than fabricating a
     * series. Empty is already a normal answer for this method, and the
     * high-value option rule degrades to underlying-only confirmation - which
     * it caps at a lower confidence anyway.
     */
    @Override
    public List<Bar> getOptionBars(String contractKey, Timeframe timeframe, int lookback) {
        return List.of();
    }

    @Override
    @RateLimiter(name = BACKEND)
    @Retry(name = BACKEND)
    @CircuitBreaker(name = BACKEND)
    public BigDecimal getPreviousClose(String ticker) {
        List<Bar> daily = fetchDaily(ticker, 2);
        // The last daily bar is today's session in progress, so the previous
        // close is the one before it.
        return daily.size() < 2 ? null : daily.get(daily.size() - 2).close();
    }

    private static List<Bar> tail(List<Bar> bars, int lookback) {
        if (lookback <= 0 || bars.size() <= lookback) {
            return bars;
        }
        return List.copyOf(bars.subList(bars.size() - lookback, bars.size()));
    }
}
