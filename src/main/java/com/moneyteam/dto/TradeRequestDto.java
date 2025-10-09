package com.moneyteam.dto;

import com.moneyteam.model.enums.TradeStatus;
import com.moneyteam.model.enums.TradeType;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradeRequestDto {
    @NotNull private TradeType tradeType;
    @NotBlank private String symbol;
    @NotNull
    @Positive private Double quantity;
    @NotNull @DecimalMin(value = "0.0, inclusive =false") private BigDecimal price;
    @NotNull private LocalDateTime executionDate;
    @NotNull private Long userId;

    public TradeStatus getStatus() {
        return status;
    }

    public void setStatus(TradeStatus status) {
        this.status = status;
    }

    @NotNull private TradeStatus status;

    public TradeType getTradeType() {
        return tradeType;
    }

    public void setTradeType(TradeType tradeType) {
        this.tradeType = tradeType;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
