package com.moneyteam.model;

public class FundamentalAnalysis {
    private String stockTicker;
    private String stockName;
    private String sector;
    private double marketCapAmount;
    private int volume;
    private double priceToEarning;
    private boolean dividend;
    private long earningPerShare;
    private int numberOfShares;
    private Stock stock; // Represents the stock being analyzed

    // Constructor
    public FundamentalAnalysis(String stockTicker, String stockName, String sector, double marketCapAmount, int volume,
                               double priceToEarning, boolean dividend, long earningPerShare, int numberOfShares)
    {
        this.stockTicker = stockTicker;
        // Initialize the other fields with default or mock values for now
        this.stockName = stockName;
        this.sector = sector;
        this.marketCapAmount = marketCapAmount;
        this.volume = volume;
        this.priceToEarning = priceToEarning;
        this.dividend = dividend;
        this.earningPerShare = earningPerShare;
        this.numberOfShares = numberOfShares;
        //this.stock = new Stock(stockTicker); // Initialize Stock class
    }

    // Getters and Setters
    public String getStockTicker() {
        return stockTicker;
    }

    public void setStockTicker(String stockTicker) {
        this.stockTicker = stockTicker;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public double getMarketCapAmount() {
        return marketCapAmount;
    }

    public void setMarketCapAmount(double marketCapAmount) {
        this.marketCapAmount = marketCapAmount;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public int getNumberOfShares() {
        return numberOfShares;
    }

    public void setNumberOfShares(int numberOfShares) {
        this.numberOfShares = numberOfShares;
    }

    public boolean hasDividend() {
        return dividend;
    }

    public void setDividend(boolean dividend) {
        this.dividend = dividend;
    }

    // Perform Analysis
//    public void performAnalysis(String tickerStockTicker) {
//        FinancialStatements financialStatements = stock.getFinancialStatements(tickerStockTicker);
//        KeyMetrics keyMetrics = stock.getKeyMetrics(tickerStockTicker);
//
//        double earningsPerShare = financialStatements.getEarningsPerShare();
//        double revenue = financialStatements.getRevenue();
//        double priceToEarningsRatio = keyMetrics.getPriceToEarningsRatio();
//
//        // Perform fundamental analysis here...
//        System.out.println("Analysis for " + stockTicker + " completed.");
//    }

    // toString Override
    @Override
    public String toString() {
        return "FundamentalAnalysis {" +
                "stockTicker='" + stockTicker + '\'' +
                ", stockName='" + stockName + '\'' +
                ", sector='" + sector + '\'' +
                ", marketCapAmount=" + marketCapAmount +
                ", volume=" + volume +
                ", priceToEarning=" + priceToEarning +
                ", dividend=" + dividend +
                ", earningPerShare=" + earningPerShare +
                ", numberOfShares=" + numberOfShares +
                '}';
    }
}
