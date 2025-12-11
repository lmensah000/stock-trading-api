package com.moneyteam.model;

import com.moneyteam.model.enums.TradeStatus;
import com.moneyteam.model.enums.TradeType;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades", indexes = {
        @Index(name = "idx_trade_user", columnList = "user_id"),
        @Index(name = "idx_trade_stockTicker", columnList = "stockTicker"),
        @Index(name = "idx_trade_date", columnList = "executionDate")
})
public class   Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id") // explicitly maps to DB column
    private Long userTradeId;

    @Column(name = "stockTicker", nullable = false)
    private String stockTicker;

    @Column(name = "position_id")
    private Long positionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type")
    private TradeType tradeType; // STOCK, OPTION

    @Enumerated(EnumType.STRING)
    private TradeStatus status;  // PENDING, EXECUTED, FAILED, CANCELLED

    private Double quantity;
    private BigDecimal price;

    @Column(name = "execution_date")
    private LocalDateTime executionDate;

    public Long getUserTradeId() {
        return userTradeId;
    }

    public void setUserTradeId(Long userTradeId) {
        this.userTradeId = userTradeId;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Position getPositions() {
        return positions;
    }

    public void setPositions(Position positions) {
        this.positions = positions;
    }

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToOne(mappedBy = "trade", cascade = CascadeType.ALL)
    private OptionTradeDetails optionDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User users;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stockTicker", referencedColumnName = "stockTicker", insertable = false, updatable = false)
    private Stock stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", insertable = false, updatable = false)
    private Position positions;

    public Trade() {};

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userTradeId;
    }

    public void setUserId(Long userId) {
        this.userTradeId = userId;
    }

    public String getStockTicker() {
        return stockTicker;
    }

    public void setStockTicker(String stockTicker) {
        this.stockTicker = stockTicker;
    }

    public TradeType getTradeType() {
        return tradeType;
    }

    public void setTradeType(TradeType tradeType) {
        this.tradeType = tradeType;
    }

    public TradeStatus getStatus() {
        return status;
    }

    public void setStatus(TradeStatus status) {
        this.status = status;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(LocalDateTime executionDate) {
        this.executionDate = executionDate;
    }

    public OptionTradeDetails getOptionDetails() {
        return optionDetails;
    }

    public void setOptionDetails(OptionTradeDetails optionDetails) {
        this.optionDetails = optionDetails;
    }

    public User getUsers() {
        return users;
    }

    public void setUsers(User users) {
        this.users = users;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public Position getPosition() {
        return positions;
    }

    public void setPosition(Position positions) {
        this.positions = positions;
    }
}
