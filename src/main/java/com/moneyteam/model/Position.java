package com.moneyteam.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(
        name = "positions",
        indexes = {
                @Index(name = "idx_position_user", columnList = "user_id"),
                @Index(name = "idx_position_stockTicker", columnList = "stockTicker")
        }
)
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Scalar FK for DTO's
    @Column(name = "user_id", nullable = false) // explicitly maps to DB column
    private Long userRefId;    // renamed local variable to avoid duplicate name

    @Column(name = "stockTicker")
    private String stockTicker;

    @Column(name = "total_quantity")
    private Double totalQuantity;

    @Column(name = "average_price")
    private BigDecimal averagePrice;

    @Column(name = "unrealized_pnl")
    private Double unrealizedPnL;
    //relationship setup
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User users;

    @OneToMany(mappedBy = "positions", cascade = CascadeType.ALL)
    private List<Trade> trades;

    public Position() {}

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userRefId;
    }

    public void setUserId(Long userId) {
        this.userRefId = userId;
    }

    public String getStockTicker() {
        return stockTicker;
    }

    public void setStockTicker(String stockTicker) {
        this.stockTicker = stockTicker;
    }

    public Double getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Double totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }

    public Double getUnrealizedPnL() {
        return unrealizedPnL;
    }

    public void setUnrealizedPnL(Double unrealizedPnL) {
        this.unrealizedPnL = unrealizedPnL;
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public void setTrades(List<Trade> trades) {
        this.trades = trades;
    }

    public Long getUserRefId() {
        return userRefId;
    }

    public void setUserRefId(Long userRefId) {
        this.userRefId = userRefId;
    }

    public User getUsers() {
        return users;
    }

    public void setUsers(User users) {
        this.users = users;
    }
}
