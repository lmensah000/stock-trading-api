package com.moneyteam.model;

import javax.persistence.*;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name", nullable = false, unique = true)
    private String userName;

    @Column(name = "pass_word",nullable = false)
    private String passWord;

    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @Column(unique = true)
    private String email;

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trade> trades;

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Position> positions;

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Watchlist> watchlists;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassWord() {
        return passWord;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    //    public String placeOrder() {
//        return placeOrder;
//    }
//
//    public void placeOrder(String placeOrder) {
//        this.placeOrder = placeOrder;
//    }

//    public String getBuy() {
//        return buy;
//    }
//
//    public void setBuy(String buy) {
//        this.buy = buy;
//    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


//    public String getSell() {
//        return sell;
//    }
//
//    public void setSell(String sell) {
//        this.sell = sell;
//    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

//    public String getPlaceOrder() {
//        return placeOrder;
//    }
//
//    public void setPlaceOrder(String placeOrder) {
//        this.placeOrder = placeOrder;
//    }

//    public List<Trade> getTrades() {
//        return trades;
//    }
//
//    public void setTrades(List<Trade> trades) {
//        this.trades = trades;
//    }

    public User() {};
    
    public User(String userName, String passWord, String email, Timestamp createdAt) {
        this.userName = userName;
        this.passWord = passWord;
//        this.placeOrder =placeHolder;
//        this.buy =buy;
//        this.sell = sell;
        this.email= email;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userName" + userName + '\'' +
                ", passWord" + passWord + '\'' +
                ", email" + email + '\'' +
                ", createdAt" + createdAt + '\'' +
                "}";
    }
}
