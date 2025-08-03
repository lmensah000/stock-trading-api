package com.moneyteam.model;

import java.text.NumberFormat;
import java.util.Locale;

public class KeyMetrics {
    private String stockTicker;
    private String stockName;
    private double priceToEarningsRatio;
    private double priceToBookRatio;
    private double dividendYield;
    private double earningsPerShare;
    private double beta;
    private double marketCap;
    private double returnOnEquity;
    private double debtToEquityRatio;
    private Double forwardPERatio;

    // Getter for stockTicker
    public String getStockTicker() {
        return stockTicker;
    }

    // Getter for stockName
    public String getStockName() {
        return stockName;
    }

    // Formatter for dollar values with commas
    private String formatDollarAmount(Double amount) {
        if (amount == null) {
            return "N/A";
        }
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        return formatter.format(amount);
    }

    // Getters and setters for each field
    public double getPriceToEarningsRatio() {
        return priceToEarningsRatio;
    }

    public void setPriceToEarningsRatio(double priceToEarningsRatio) {
        this.priceToEarningsRatio = priceToEarningsRatio;
    }

    // Add similar methods for other metrics
}
