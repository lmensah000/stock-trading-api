package com.moneyteam.model;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "option_trade_details")
public class OptionTradeDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double strikePrice;
    private LocalDate expirationDate;
    private String optionType; // CALL or PUT

    @OneToOne
    @JoinColumn(name = "trade_id", nullable = false)
    private Trade trade;

    // Getters and setters
}
