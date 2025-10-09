package com.moneyteam.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_user", columnList = "userId"),
        @Index(name = "idx_order_symbol", columnList = "symbol")
})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String symbol;
    private Double quantity;
    private Double targetPrice;

    @Enumerated(EnumType.STRING)
    private OrderType orderType; // MARKET, LIMIT, STOP

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "trade_id")
    private Trade executedTrade;

    // Getters and setters
}
