package com.moneyteam.trading.model;

import com.moneyteam.trading.model.enums.OrderSide;

public class StockTradeRequest {
    private String stockTicker;
    private int quantity;
    private OrderSide tradeType;
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

    public OrderSide getTradeType() {
        return tradeType;
    }

    public void setTradeType(OrderSide tradeType) {
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
