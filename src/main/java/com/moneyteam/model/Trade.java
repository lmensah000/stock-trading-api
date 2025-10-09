package com.moneyteam.model;

import com.moneyteam.model.enums.TradeStatus;
import com.moneyteam.model.enums.TradeType;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades", indexes = {
        @Index(name = "idx_trade_user", columnList = "userId"),
        @Index(name = "idx_trade_symbol", columnList = "symbol"),
        @Index(name = "idx_trade_date", columnList = "executionDate")
})
public class   Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String symbol;

    @Enumerated(EnumType.STRING)
    private TradeType tradeType; // STOCK, OPTION

    @Enumerated(EnumType.STRING)
    private TradeStatus status;  // PENDING, EXECUTED, FAILED

    private Double quantity;
    private BigDecimal price;

    private LocalDateTime executionDate;

    @OneToOne(mappedBy = "trade", cascade = CascadeType.ALL)
    private OptionTradeDetails optionDetails;

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

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
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
// Getters and setters
}
