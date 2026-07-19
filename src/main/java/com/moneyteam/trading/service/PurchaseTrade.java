package com.moneyteam.trading.service;

import com.moneyteam.marketdata.model.Stock;
import com.moneyteam.user.model.User;
import com.moneyteam.analysis.service.StockStrategies;

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

    public void executeStockTrade(User users, Stock stock, StockStrategies strategy) {
        // Logic for executing a stock trade based on the provided strategy
        // Use the stockApiService to interact with the API for trading
    }
}
