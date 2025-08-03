package com.moneyteam.model;

public class FinancialStatements {
    private double earningsPerShare;
    private double revenue;
    private double grossProfit;
    private double netIncome;
    private double totalAssets;
    private double totalLiabilities;
    private double shareholderEquity;
    private double operatingCashFlow;
    private double freeCashFlow;


    public FinancialStatements(double earningsPerShare, double revenue) {
        this.earningsPerShare = earningsPerShare;
        this.revenue = revenue;
    }

    public double getEarningsPerShare() {
        return earningsPerShare;
    }

    public void setEarningsPerShare(double earningsPerShare) {
        this.earningsPerShare = earningsPerShare;
    }

    public double getRevenue() {
        return revenue;
    }

    public void setRevenue(double revenue) {
        this.revenue = revenue;
    }
}
