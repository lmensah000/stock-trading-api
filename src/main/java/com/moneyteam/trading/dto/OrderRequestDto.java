package com.moneyteam.trading.dto;

import java.math.BigDecimal;
import com.moneyteam.trading.model.OrderType;
import com.moneyteam.trading.model.enums.OrderSide;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class OrderRequestDto {

    // No userId here on purpose: the acting user comes from the authenticated
    // principal (CurrentUserService), never from the request body.

    @NotBlank
    private String stockTicker;

    @NotNull
    @Positive
    private Double quantity;

    private BigDecimal targetPrice;

    @NotNull
    private OrderType orderType;

    @NotNull
    private OrderSide side;

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

    public BigDecimal getTargetPrice() {
        return targetPrice;
    }

    public void setTargetPrice(BigDecimal targetPrice) {
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
