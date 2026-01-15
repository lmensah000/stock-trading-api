package com.moneyteam.model;

import com.moneyteam.model.enums.OptionType;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Entity representing an options contract.
 */
@Entity
@Table(name = "options")
public class Options {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_ticker")
    private String stockTicker;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type")
    private OptionType optionType;

    @Column(name = "strike_price")
    private BigDecimal strikePrice;

    @Column(name = "expiration")
    private String expiration;

    @Column(name = "user_id")
    private Long userRefId;

    @Column(name = "premium")
    private BigDecimal premium;

    @Column(name = "contracts")
    private Integer contracts;

    // Relationship (read-only)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User users;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_ticker", referencedColumnName = "stockTicker", insertable = false, updatable = false)
    private Stock stock;

    public Options() {}

    public Options(String stockTicker, OptionType optionType, BigDecimal strikePrice, String expiration) {
        this.stockTicker = stockTicker;
        this.optionType = optionType;
        this.strikePrice = strikePrice;
        this.expiration = expiration;
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

    public String getExpiration() {
        return expiration;
    }

    public void setExpiration(String expiration) {
        this.expiration = expiration;
    }

    public Long getUserRefId() {
        return userRefId;
    }

    public void setUserRefId(Long userRefId) {
        this.userRefId = userRefId;
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

    @Override
    public String toString() {
        return "Options{" +
                "id=" + id +
                ", stockTicker='" + stockTicker + '\'' +
                ", optionType=" + optionType +
                ", strikePrice=" + strikePrice +
                ", expiration='" + expiration + '\'' +
                ", premium=" + premium +
                ", contracts=" + contracts +
                '}';
    }
}
