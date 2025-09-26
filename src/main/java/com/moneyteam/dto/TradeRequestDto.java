package com.moneyteam.dto;

import com.moneyteam.model.TradeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradeRequestDto {
    @NotNull private TradeType tradeType;
    @NotBlank private String symbol;
    @NotNull @Positive private Integer quantity;
    @NotNull @DecimalMin(value = "0.0, inclusive =false") private BigDecimal price;
    @NotNull private LocalDateTime executionDate;
    @NotNull private Long userId;

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

    public Integer getQuanitty() {
        return quanitty;
    }

    public void setQuanitty(Integer quanitty) {
        this.quanitty = quanitty;
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
