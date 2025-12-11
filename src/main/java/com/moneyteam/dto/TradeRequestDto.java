package com.moneyteam.dto;

import com.moneyteam.model.enums.TradeStatus;
import com.moneyteam.model.enums.TradeType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradeRequestDto {
    @NotNull
    private TradeType tradeType;
    @NotBlank private String stockTicker;
    @NotNull
    @Positive private Double quantity;
    @NotNull @DecimalMin(value = "0.0", inclusive =false) private BigDecimal price;
    @NotNull private LocalDateTime executionDate;
    @NotNull private Long userTradeId;

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
        return userTradeId;
    }

    public void setUserId(Long userId) {
        this.userTradeId = userId;
    }
}
