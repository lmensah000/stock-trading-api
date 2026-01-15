package com.moneyteam.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Request object for stock trading operations.
 */
public class StockTradeRequest {

    @NotBlank(message = "Stock ticker is required")
    private String stockTicker;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Double quantity;

    @NotBlank(message = "Trade type is required (BUY or SELL)")
    private String tradeType;

    private BigDecimal limitPrice;

    private Long userId;

    private String orderType; // MARKET, LIMIT, STOP

    public StockTradeRequest() {}

    public StockTradeRequest(String stockTicker, Double quantity, String tradeType, Long userId) {
        this.stockTicker = stockTicker;
        this.quantity = quantity;
        this.tradeType = tradeType;
        this.userId = userId;
        this.orderType = "MARKET";
    }

    // Getters and Setters
    public String getStockTicker() {
        return stockTicker;
    }

    public void setStockTicker(String stockTicker) {
        this.stockTicker = stockTicker;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public BigDecimal getLimitPrice() {
        return limitPrice;
    }

    public void setLimitPrice(BigDecimal limitPrice) {
        this.limitPrice = limitPrice;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    @Override
    public String toString() {
        return "StockTradeRequest{" +
                "stockTicker='" + stockTicker + '\'' +
                ", quantity=" + quantity +
                ", tradeType='" + tradeType + '\'' +
                ", limitPrice=" + limitPrice +
                ", userId=" + userId +
                ", orderType='" + orderType + '\'' +
                '}';
    }
}
