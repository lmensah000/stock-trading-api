package com.moneyteam.model;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "positions")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String symbol;
    private Double totalQuantity;
    private Double averagePrice;
    private Double unrealizedPnL;

    @OneToMany(mappedBy = "position", cascade = CascadeType.ALL)
    private List<Trade> trades;

    // Getters and setters
}
