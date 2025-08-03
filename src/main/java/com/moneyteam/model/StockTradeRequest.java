package com.moneyteam.model;

public class StockTradeRequest {
    private String stockTicker;
    private int quantity;
    private String tradeType; // "BUY" or "SELL"
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

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
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
