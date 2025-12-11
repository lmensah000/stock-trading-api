package com.moneyteam.model;

import com.moneyteam.model.enums.TradeType;

public class StockTradeRequest {
    private String stockTicker;
    private int quantity;
    private TradeType tradeType; // "BUY" or "SELL"
    private Long userId;

    // Getters and setters
    public String getStockTicker() {
        return stockTicker;
    }

    public void setStockTicker(String stockTicker) {
        this.stockTicker = stockTicker;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public TradeType getTradeType() {
        return tradeType;
    }

    public void setTradeType(TradeType tradeType) {
        this.tradeType = tradeType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "StockTradeRequest{" +
                "stockTicker='" + stockTicker + '\'' +
                ", quantity=" + quantity +
                ", tradeType='" + tradeType + '\'' +
                ", userId=" + userId +
                '}';
    }
}
