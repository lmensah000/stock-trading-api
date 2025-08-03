package com.moneyteam.service;

import com.moneyteam.model.Stock;
import com.moneyteam.model.User;

public class PurchaseTrade {

    private double stockPrice;

    private double fees;

    public double getFees() {
        return fees;
    }

    public void setFees(double fees) {
        this.fees = fees;
    }

    public double getStockPrice() {
        return stockPrice;
    }

    public void setStockPrice(double stockPrice) {
        this.stockPrice = stockPrice;
    }

    public void executeStockTrade(User user, Stock stock, StockStrategies strategy) {
        // Logic for executing a stock trade based on the provided strategy
        // Use the stockApiService to interact with the API for trading
    }
}
