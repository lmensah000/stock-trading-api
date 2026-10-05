package com.moneyteam.marketdata.service.impl;

import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.OptionQuote;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.marketdata.service.MarketDataProvider;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A market-data provider backed by whatever is loaded into it.
 *
 * This exists so the strategy engine and its rules can be built and tested in
 * full before a real feed exists. It is not a stub in the sense of returning
 * canned nonsense - it returns exactly the bars it was given, honouring the
 * same ordering and immutability contract a live provider must honour, which
 * is what makes tests against it meaningful.
 *
 * Thread-safe: backed by concurrent maps holding unmodifiable lists, so a scan
 * running one task per ticker reads it without coordination.
 */
public class InMemoryMarketDataProvider implements MarketDataProvider {

    private final Map<String, List<Bar>> bars = new ConcurrentHashMap<>();
    private final Map<String, List<Bar>> optionBars = new ConcurrentHashMap<>();
    private final Map<String, List<OptionQuote>> chains = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> previousCloses = new ConcurrentHashMap<>();

    private static String key(String id, Timeframe timeframe) {
        return id + "@" + timeframe.name();
    }

    /** Loads bars for a ticker. The list is validated as oldest-first and copied. */
    public InMemoryMarketDataProvider putBars(String ticker, Timeframe timeframe, List<Bar> series) {
        Bar.requireOldestFirst(series);
        bars.put(key(ticker, timeframe), List.copyOf(series));
        return this;
    }

    /** Loads bars for one option contract, keyed by {@link OptionQuote#contractKey()}. */
    public InMemoryMarketDataProvider putOptionBars(String contractKey, Timeframe timeframe, List<Bar> series) {
        Bar.requireOldestFirst(series);
        optionBars.put(key(contractKey, timeframe), List.copyOf(series));
        return this;
    }

    public InMemoryMarketDataProvider putOptionChain(String ticker, List<OptionQuote> chain) {
        chains.put(ticker, List.copyOf(chain));
        return this;
    }

    public InMemoryMarketDataProvider putPreviousClose(String ticker, BigDecimal close) {
        previousCloses.put(ticker, close);
        return this;
    }

    @Override
    public List<Bar> getBars(String ticker, Timeframe timeframe, int lookback) {
        return tail(bars.getOrDefault(key(ticker, timeframe), List.of()), lookback);
    }

    @Override
    public List<Bar> getOptionBars(String contractKey, Timeframe timeframe, int lookback) {
        return tail(optionBars.getOrDefault(key(contractKey, timeframe), List.of()), lookback);
    }

    @Override
    public List<OptionQuote> getOptionChain(String ticker) {
        return chains.getOrDefault(ticker, List.of());
    }

    @Override
    public BigDecimal getPreviousClose(String ticker) {
        return previousCloses.get(ticker);
    }

    /** Most recent {@code lookback} bars, still oldest-first. */
    private static List<Bar> tail(List<Bar> series, int lookback) {
        if (lookback <= 0 || series.isEmpty()) {
            return List.of();
        }
        if (series.size() <= lookback) {
            return series;
        }
        return Collections.unmodifiableList(
                new ArrayList<>(series.subList(series.size() - lookback, series.size())));
    }
}
