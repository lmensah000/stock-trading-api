package com.moneyteam.model;

import com.moneyteam.service.StockStrategies;

public class OptionsTradeRequest {
    private String stockTicker;
    private int quantity;
    private String tradeType; // "BUY" or "SELL"
    private Long userTradeId;

    private Options options;
    private StockStrategies strategy;


    public Long getUserId() {
        return userTradeId;
    }

    public void setUserId(Long userId) {
        this.userTradeId = userId;
    }

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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

}
