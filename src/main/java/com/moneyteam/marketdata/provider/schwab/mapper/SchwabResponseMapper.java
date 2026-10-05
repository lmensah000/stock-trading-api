package com.moneyteam.marketdata.provider.schwab.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.OptionQuote;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.trading.model.OptionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Maps Schwab JSON onto the domain value types.
 *
 * Prices are read with {@link JsonNode#decimalValue()} rather than
 * {@code asDouble()}: routing them through a double would round before they
 * ever reached a {@link BigDecimal}, which defeats the point of using one.
 */
public final class SchwabResponseMapper {

    /** US equity sessions close at 16:00 ET; open interest is published after. */
    private static final ZoneId EXCHANGE_ZONE = ZoneId.of("America/New_York");

    private SchwabResponseMapper() {
    }

    /**
     * Price history: {@code {"candles":[{open,high,low,close,volume,datetime}]}}.
     * {@code datetime} is epoch milliseconds.
     *
     * Schwab returns candles oldest-first, which matches the ordering invariant;
     * it is asserted rather than assumed, because a silent reversal would turn
     * every bullish gap into a bearish one.
     */
    public static List<Bar> toBars(JsonNode root, String ticker, Timeframe timeframe) {
        JsonNode candles = root.path("candles");
        if (!candles.isArray() || candles.isEmpty()) {
            return List.of();
        }

        List<Bar> bars = new ArrayList<>(candles.size());
        for (JsonNode candle : candles) {
            bars.add(new Bar(
                    ticker,
                    Instant.ofEpochMilli(candle.path("datetime").asLong()),
                    timeframe,
                    decimal(candle, "open"),
                    decimal(candle, "high"),
                    decimal(candle, "low"),
                    decimal(candle, "close"),
                    candle.path("volume").asLong()));
        }

        Bar.requireOldestFirst(bars);
        return List.copyOf(bars);
    }

    /**
     * Option chain: {@code callExpDateMap} / {@code putExpDateMap}, each a map
     * of "expiry:daysToExpiry" to a map of strike to a single-element array of
     * contracts.
     *
     * @param openInterestAsOf when the open interest figure was published.
     *                         Supplied by the caller because the response does
     *                         not carry it - see
     *                         {@code SchwabMarketDataProvider} for why that
     *                         distinction is preserved rather than papered over.
     */
    public static List<OptionQuote> toOptionQuotes(JsonNode root,
                                                   String underlying,
                                                   Instant asOf,
                                                   Instant openInterestAsOf) {
        List<OptionQuote> quotes = new ArrayList<>();
        collect(root.path("callExpDateMap"), underlying, OptionType.CALL, asOf, openInterestAsOf, quotes);
        collect(root.path("putExpDateMap"), underlying, OptionType.PUT, asOf, openInterestAsOf, quotes);
        return List.copyOf(quotes);
    }

    private static void collect(JsonNode expiryMap,
                                String underlying,
                                OptionType type,
                                Instant asOf,
                                Instant openInterestAsOf,
                                List<OptionQuote> into) {
        if (!expiryMap.isObject()) {
            return;
        }

        Iterator<Map.Entry<String, JsonNode>> expiries = expiryMap.fields();
        while (expiries.hasNext()) {
            Map.Entry<String, JsonNode> expiryEntry = expiries.next();
            LocalDate expiration = parseExpiry(expiryEntry.getKey());

            Iterator<Map.Entry<String, JsonNode>> strikes = expiryEntry.getValue().fields();
            while (strikes.hasNext()) {
                for (JsonNode contract : strikes.next().getValue()) {
                    into.add(toQuote(contract, underlying, type, expiration, asOf, openInterestAsOf));
                }
            }
        }
    }

    private static OptionQuote toQuote(JsonNode contract,
                                       String underlying,
                                       OptionType type,
                                       LocalDate expiration,
                                       Instant asOf,
                                       Instant openInterestAsOf) {
        return new OptionQuote(
                underlying,
                type,
                decimal(contract, "strikePrice"),
                expiration,
                decimal(contract, "bid"),
                decimal(contract, "ask"),
                decimal(contract, "last"),
                contract.path("totalVolume").asLong(),
                contract.path("openInterest").asLong(),
                asOf,
                openInterestAsOf);
    }

    /** Keys look like "2024-03-15:11" - the suffix is days to expiry. */
    private static LocalDate parseExpiry(String key) {
        int colon = key.indexOf(':');
        return LocalDate.parse(colon > 0 ? key.substring(0, colon) : key);
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.decimalValue();
    }

    /**
     * The close of the session before {@code now}, in exchange time - the point
     * at which the currently published open interest was struck.
     */
    public static Instant previousSessionClose(Instant now) {
        var exchangeNow = now.atZone(EXCHANGE_ZONE);
        var today4pm = exchangeNow.toLocalDate().atTime(16, 0).atZone(EXCHANGE_ZONE);

        var close = exchangeNow.isAfter(today4pm) ? today4pm : today4pm.minusDays(1);

        // Step back over a weekend. Holidays need the trading calendar, which
        // does not exist yet; the result is then at worst a day conservative,
        // which makes the staleness check stricter rather than looser.
        while (close.getDayOfWeek().getValue() > 5) {
            close = close.minusDays(1);
        }
        return close.toInstant();
    }
}
