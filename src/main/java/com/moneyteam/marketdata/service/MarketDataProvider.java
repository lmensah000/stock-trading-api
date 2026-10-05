package com.moneyteam.marketdata.service;

import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.OptionQuote;
import com.moneyteam.marketdata.model.Timeframe;

import java.util.List;

/**
 * The port through which the strategy engine obtains market data.
 *
 * Implementations are called concurrently - one task per ticker during a scan -
 * so every implementation must be thread-safe. The returned collections must be
 * effectively immutable; callers share them across threads without copying.
 *
 * No live provider exists yet. {@code InMemoryMarketDataProvider} serves tests
 * and local runs; a real adapter with rate limiting, retry and circuit breaking
 * arrives with the market-data phase, and nothing above this interface changes
 * when it does.
 */
public interface MarketDataProvider {

    /**
     * Price bars for a ticker, <strong>oldest-first</strong>: index 0 is the
     * earliest bar and {@code size()-1} the most recent. Every indicator and
     * detector depends on this ordering.
     *
     * @param lookback maximum number of bars to return, most recent
     * @return the bars, or an empty list when the ticker is unknown
     */
    List<Bar> getBars(String ticker, Timeframe timeframe, int lookback);

    /**
     * The current option chain for an underlying.
     *
     * @return every quoted contract, or an empty list when none is available
     */
    List<OptionQuote> getOptionChain(String ticker);

    /**
     * Bars for a single option contract, keyed by {@link OptionQuote#contractKey()}.
     *
     * Separated from {@link #getBars} because option contract bars are a
     * different and much thinner data set: many intervals have no trades at
     * all, and a provider may not offer them. An empty list is a normal
     * answer here, not an error.
     */
    List<Bar> getOptionBars(String contractKey, Timeframe timeframe, int lookback);

    /** The most recent daily close prior to the current session, if known. */
    java.math.BigDecimal getPreviousClose(String ticker);
}
