package com.moneyteam.model;

import com.moneyteam.service.StockStrategies;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request object for options trading operations.
 */
public class OptionsTradeRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Options details are required")
    private Options options;

    @NotNull(message = "Trading strategy is required")
    private StockStrategies strategy;

    private Double quantity;

    private BigDecimal limitPrice;

    private String orderType; // MARKET, LIMIT

    public OptionsTradeRequest() {}

    public OptionsTradeRequest(Long userId, Options options, StockStrategies strategy) {
        this.userId = userId;
        this.options = options;
        this.strategy = strategy;
        this.orderType = "MARKET";
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public void setStrategy(StockStrategies strategy) {
        this.strategy = strategy;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLimitPrice() {
        return limitPrice;
    }

    public void setLimitPrice(BigDecimal limitPrice) {
        this.limitPrice = limitPrice;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    @Override
    public String toString() {
        return "OptionsTradeRequest{" +
                "userId=" + userId +
                ", options=" + options +
                ", strategy=" + strategy +
                ", quantity=" + quantity +
                ", limitPrice=" + limitPrice +
                ", orderType='" + orderType + '\'' +
                '}';
    }
}
