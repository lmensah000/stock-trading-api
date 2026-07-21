package com.moneyteam.trading.dto;

import com.moneyteam.trading.model.OrderType;
import com.moneyteam.trading.model.enums.OrderSide;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class OrderRequestDto {

    @NotNull
    private Long userId;

    @NotBlank
    private String stockTicker;

    @NotNull
    @Positive
    private Double quantity;

    private Double targetPrice;

    @NotNull
    private OrderType orderType;

    @NotNull
    private OrderSide side;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

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

    public Double getTargetPrice() {
        return targetPrice;
    }

    public void setTargetPrice(Double targetPrice) {
        this.targetPrice = targetPrice;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public OrderSide getSide() {
        return side;
    }

    public void setSide(OrderSide side) {
        this.side = side;
    }
}
