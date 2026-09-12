package com.moneyteam.trading.dto;

import com.moneyteam.trading.model.OrderType;
import com.moneyteam.trading.model.enums.OrderSide;
import com.moneyteam.trading.model.enums.OrderStatus;

import java.time.LocalDateTime;

public class OrderResponseDto {

    private Long id;
    private Long userId;
    private String stockTicker;
    private Double quantity;
    private Double filledQuantity;
    private Double targetPrice;
    private OrderType orderType;
    private OrderSide side;
    private OrderStatus status;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Double getFilledQuantity() {
        return filledQuantity;
    }

    public void setFilledQuantity(Double filledQuantity) {
        this.filledQuantity = filledQuantity;
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

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
