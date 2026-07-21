package com.moneyteam.trading.dto;

import java.math.BigDecimal;

public class AccountResponseDto {

    private Long userId;
    private BigDecimal cashBalance;
    private BigDecimal buyingPower;

    public AccountResponseDto() {}

    public AccountResponseDto(Long userId, BigDecimal cashBalance, BigDecimal buyingPower) {
        this.userId = userId;
        this.cashBalance = cashBalance;
        this.buyingPower = buyingPower;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }

    public BigDecimal getBuyingPower() {
        return buyingPower;
    }

    public void setBuyingPower(BigDecimal buyingPower) {
        this.buyingPower = buyingPower;
    }
}
