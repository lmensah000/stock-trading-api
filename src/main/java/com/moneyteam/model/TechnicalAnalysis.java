package com.moneyteam.model;

public class TechnicalAnalysis {
    public  String stockTicker;
    public  String stockName;
    public  String sector;
    public  long marketCapAmount;
    public  int volume;
    public  double sizzle_Index;
    public  int numberOfShares;
    public  double open;
    public double close;
    public double last;

    public  String getStockTicker() {
        return stockTicker;
    }

    public void setStockTicker(String stockTicker) {
        this.stockTicker = stockTicker;
    }

    public  String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public  String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public  long getMarketCap() {
        return marketCapAmount;
    }

    public  void setMarketCap(long marketCap) {
        TechnicalAnalysis.marketCapAmount = marketCap;
    }

    public  int getVolume() {
        return volume;
    }

    public  void setVolume(int volume) {
        TechnicalAnalysis.volume = volume;
    }

    public  double getSizzle_Index() {
        return sizzle_Index;
    }

    public  void setSizzle_Index(double sizzle_Index) {
        TechnicalAnalysis.sizzle_Index = sizzle_Index;
    }

    public  int getShares() {
        return numberOfShares;
    }

    public  void setShares(int shares) {
        TechnicalAnalysis.numberOfShares = shares;
    }

    public  double getOpen() {
        return open;
    }

    public  void setOpen(double open) {
        TechnicalAnalysis.open = open;
    }

    public  double getClose() {
        return close;
    }

    public  void setClose(double close) {
        TechnicalAnalysis.close = close;
    }

    public  double getLast() {
        return last;
    }

    public  void setLast(double last) {
        TechnicalAnalysis.last = last;
    }

    public TechnicalAnalysis(String stockTicker, double last, String stockName, String sector, int marketCap,
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
