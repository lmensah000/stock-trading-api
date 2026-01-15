package com.moneyteam.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a stock purchase trade with execution details.
 */
@Entity
@Table(name = "purchase_trades")
public class PurchaseTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_ticker", nullable = false)
    private String stockTicker;

    @Column(name = "stock_price", nullable = false)
    private BigDecimal stockPrice;

    @Column(name = "quantity", nullable = false)
    private Double quantity;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "execution_time")
    private LocalDateTime executionTime;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    public PurchaseTrade() {
        this.executionTime = LocalDateTime.now();
    }

    public PurchaseTrade(String stockTicker, BigDecimal stockPrice, Double quantity, Long userId) {
        this.stockTicker = stockTicker;
        this.stockPrice = stockPrice;
        this.quantity = quantity;
        this.userId = userId;
        this.totalAmount = stockPrice.multiply(BigDecimal.valueOf(quantity));
        this.executionTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStockTicker() {
        return stockTicker;
    }

    public void setStockTicker(String stockTicker) {
        this.stockTicker = stockTicker;
    }

    public BigDecimal getStockPrice() {
        return stockPrice;
    }

    public void setStockPrice(BigDecimal stockPrice) {
        this.stockPrice = stockPrice;
        calculateTotalAmount();
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
        calculateTotalAmount();
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(LocalDateTime executionTime) {
        this.executionTime = executionTime;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    private void calculateTotalAmount() {
        if (this.stockPrice != null && this.quantity != null) {
            this.totalAmount = this.stockPrice.multiply(BigDecimal.valueOf(this.quantity));
        }
    }

    @Override
    public String toString() {
        return "PurchaseTrade{" +
                "id=" + id +
                ", stockTicker='" + stockTicker + '\'' +
                ", stockPrice=" + stockPrice +
                ", quantity=" + quantity +
                ", totalAmount=" + totalAmount +
                ", executionTime=" + executionTime +
                ", userId=" + userId +
                '}';
    }
}
