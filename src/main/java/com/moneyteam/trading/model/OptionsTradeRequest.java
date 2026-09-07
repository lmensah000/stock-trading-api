package com.moneyteam.trading.model;

import com.moneyteam.analysis.service.StockStrategies;

public class OptionsTradeRequest {
    private String stockTicker;
    private int quantity;
    private String tradeType; // "BUY" or "SELL"

    // No userId: the acting user comes from the authenticated principal.

    private Options options;
    private StockStrategies strategy;

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public String getStockTicker() {
        return stockTicker;
    }

    public void setStockTicker(String stockTicker) {
        this.stockTicker = stockTicker;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    public StockStrategies getStrategy() {
        return strategy;
    }

    // Without this setter Jackson cannot populate strategy, so every request
    // arrived with it null and was rejected by the controller's validation.
    public void setStrategy(StockStrategies strategy) {
        this.strategy = strategy;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

}
