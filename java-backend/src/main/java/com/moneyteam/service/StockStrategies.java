package com.moneyteam.service;

/**
 * Enum representing different stock trading strategies.
 */
public enum StockStrategies {
    
    // Basic Strategies
    BUY_AND_HOLD("Buy and Hold", "Long-term investment strategy"),
    DAY_TRADING("Day Trading", "Intraday trading strategy"),
    SWING_TRADING("Swing Trading", "Medium-term trading strategy"),
    SCALPING("Scalping", "Quick profit strategy with small price changes"),
    
    // Options Strategies
    COVERED_CALL("Covered Call", "Sell call options on owned stock"),
    PROTECTIVE_PUT("Protective Put", "Buy put options to protect long position"),
    IRON_CONDOR("Iron Condor", "Neutral strategy with limited risk"),
    STRADDLE("Straddle", "Buy both call and put at same strike"),
    STRANGLE("Strangle", "Buy call and put at different strikes"),
    BULL_CALL_SPREAD("Bull Call Spread", "Bullish limited risk strategy"),
    BEAR_PUT_SPREAD("Bear Put Spread", "Bearish limited risk strategy"),
    BUTTERFLY("Butterfly", "Neutral strategy for low volatility"),
    
    // Technical Analysis Based
    MOMENTUM("Momentum", "Trade based on price momentum"),
    MEAN_REVERSION("Mean Reversion", "Trade on price returning to average"),
    BREAKOUT("Breakout", "Trade on price breaking key levels"),
    
    // Value Based
    VALUE_INVESTING("Value Investing", "Invest in undervalued stocks"),
    GROWTH_INVESTING("Growth Investing", "Invest in high-growth stocks"),
    DIVIDEND_INVESTING("Dividend Investing", "Invest for dividend income");

    private final String name;
    private final String description;

    StockStrategies(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name + ": " + description;
    }
}
