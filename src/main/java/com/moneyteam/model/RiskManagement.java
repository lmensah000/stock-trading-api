package com.moneyteam.model;

public class RiskManagement {
    public static String stockTicker;
    public static String stockName;
    public static String sector;
    public static long marketCapAmount;
    public static int volume;
    public static double sizzle_Index;
    public static int numberOfShares;
    public static double open, close, last;

    public static String getStockTicker() {
        return stockTicker;
    }

    public void setStockTicker(String stockTicker) {
        this.stockTicker = stockTicker;
    }

    public static String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public static String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public static long getMarketCap() {
        return marketCapAmount;
    }

    public static void setMarketCap(long marketCap) {
        RiskManagement.marketCapAmount = marketCap;
    }

    public static int getVolume() {
        return volume;
    }

    public static void setVolume(int volume) {
        RiskManagement.volume = volume;
    }

    public static double getSizzle_Index() {
        return sizzle_Index;
    }

    public static void setSizzle_Index(double sizzle_Index) {
        RiskManagement.sizzle_Index = sizzle_Index;
    }

    public static int getShares() {
        return numberOfShares;
    }

    public static void setShares(int shares) {
        RiskManagement.numberOfShares = shares;
    }

    public static double getOpen() {
        return open;
    }

    public static void setOpen(double open) {
        RiskManagement.open = open;
    }

    public static double getClose() {
        return close;
    }

    public static void setClose(double close) {
        RiskManagement.close = close;
    }

    public static double getLast() {
        return last;
    }

    public static void setLast(double last) {
        RiskManagement.last = last;
    }

    public RiskManagement(String stockTicker, double last, String stockName, String sector, int marketCap,
                          int volume, double sizzle_Index, int shares, double open, double close) {
        
        this.stockTicker = stockTicker;
        this.last = last;
        this.stockName = stockName;
        this.sector = sector;
        this.marketCapAmount = marketCap;
        this.volume = volume;
        this.sizzle_Index = sizzle_Index;
        this.numberOfShares = shares;
        this.open = open;
        this.close = close;
    }

//    for(int i = 0; i < INVENTORY_SIZE; i++) {
//        stock[i] = new Stock(String stockTicker, double last, String stockName, String sector, int marketCap,
//        int volume, double sizzle_Index, int shares, double open, double close);
//    }
//    int stockdata = stock[0].getPrice();
    @Override
    public String toString() {
        return "Stock{" +
                "ticker" + stockTicker +
                "last" + last +
                "name" + stockName +
                "stockSector" + sector +
                "marketCap" + marketCapAmount +
                "volume" + volume +
                "sizzleIndex" + sizzle_Index +
                "shares" + numberOfShares +
                "open" + open +
                "close" + close +
        "}";
    }
}
