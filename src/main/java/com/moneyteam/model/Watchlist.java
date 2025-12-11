package com.moneyteam.model;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "watchlists.sql")
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id") // explicitly maps to DB column
    private Long userWatchListId;

    @ManyToMany
    @JoinTable(
        name = "watchlist_stocks",
        joinColumns = @JoinColumn(name = "watchlist_id"),
        inverseJoinColumns = @JoinColumn(name = "stock_stockTicker")
    )
    private List<Stock> stocks;

    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User users;
    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userWatchListId;
    }

    public void setUserId(Long userId) {
        this.userWatchListId = userId;
    }

    public List<Stock> getStocks() {
        return stocks;
    }

    public void setStocks(List<Stock> stocks) {
        this.stocks = stocks;
    }

    public User getUsers() {
        return users;
    }

    public void setUsers(User users) {
        this.users = users;
    }
}
