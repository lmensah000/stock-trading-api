package com.moneyteam.model;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "options")
public class Options {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_ticker")
    private String stockTicker;

    @Enumerated(EnumType.STRING)
    private OptionType optionType;

    @Column(name = "strike_price")
    private BigDecimal strikePrice;

    @Column(name = "expiration")
    private String expiration;

    @Column(name = "user_id")
    private Long userRefId;

    // ✅ relationship (read-only).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User users;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_ticker", referencedColumnName = "stockTicker", insertable = false, updatable = false)
    private Stock stock;


}
