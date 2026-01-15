package com.moneyteam.model;

import com.moneyteam.model.enums.OptionType;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing details of an options trade.
 */
@Entity
@Table(name = "option_trade_details")
public class OptionTradeDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", nullable = false)
    private OptionType optionType;

    @Column(name = "strike_price", nullable = false)
    private BigDecimal strikePrice;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Column(name = "premium")
    private BigDecimal premium;

    @Column(name = "contracts")
    private Integer contracts;

    @Column(name = "underlying_ticker")
    private String underlyingTicker;

    public OptionTradeDetails() {}

    public OptionTradeDetails(Trade trade, OptionType optionType, BigDecimal strikePrice, 
                              LocalDate expirationDate, BigDecimal premium, Integer contracts) {
        this.trade = trade;
        this.optionType = optionType;
        this.strikePrice = strikePrice;
        this.expirationDate = expirationDate;
        this.premium = premium;
        this.contracts = contracts;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Trade getTrade() {
        return trade;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
    }

    public OptionType getOptionType() {
        return optionType;
    }

    public void setOptionType(OptionType optionType) {
        this.optionType = optionType;
    }

    public BigDecimal getStrikePrice() {
        return strikePrice;
    }

    public void setStrikePrice(BigDecimal strikePrice) {
        this.strikePrice = strikePrice;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public BigDecimal getPremium() {
        return premium;
    }

    public void setPremium(BigDecimal premium) {
        this.premium = premium;
    }

    public Integer getContracts() {
        return contracts;
    }

    public void setContracts(Integer contracts) {
        this.contracts = contracts;
    }

    public String getUnderlyingTicker() {
        return underlyingTicker;
    }

    public void setUnderlyingTicker(String underlyingTicker) {
        this.underlyingTicker = underlyingTicker;
    }

    @Override
    public String toString() {
        return "OptionTradeDetails{" +
                "id=" + id +
                ", optionType=" + optionType +
                ", strikePrice=" + strikePrice +
                ", expirationDate=" + expirationDate +
                ", premium=" + premium +
                ", contracts=" + contracts +
                ", underlyingTicker='" + underlyingTicker + '\'' +
                '}';
    }
}
