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

    public double getGrossProfit() {
        return grossProfit;
    }

    public void setGrossProfit(double grossProfit) {
        this.grossProfit = grossProfit;
    }

    public double getNetIncome() {
        return netIncome;
    }

    public void setNetIncome(double netIncome) {
        this.netIncome = netIncome;
    }

    public double getTotalAssets() {
        return totalAssets;
    }

    public void setTotalAssets(double totalAssets) {
        this.totalAssets = totalAssets;
    }

    public double getTotalLiabilities() {
        return totalLiabilities;
    }

    public void setTotalLiabilities(double totalLiabilities) {
        this.totalLiabilities = totalLiabilities;
    }

    public double getShareholderEquity() {
        return shareholderEquity;
    }

    public void setShareholderEquity(double shareholderEquity) {
        this.shareholderEquity = shareholderEquity;
    }

    public double getOperatingCashFlow() {
        return operatingCashFlow;
    }

    public void setOperatingCashFlow(double operatingCashFlow) {
        this.operatingCashFlow = operatingCashFlow;
    }

    public double getFreeCashFlow() {
        return freeCashFlow;
    }

    public void setFreeCashFlow(double freeCashFlow) {
        this.freeCashFlow = freeCashFlow;
    }
}
