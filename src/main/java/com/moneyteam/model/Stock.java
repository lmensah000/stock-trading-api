package com.moneyteam.model;

import javax.persistence.*;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "stocks")
public class Stock {
    // Instance variables
    @JsonProperty(required = true)
    @Id
    @Column(name = "stock_ticker")
    private String stockTicker;

    @Column(name = "stock_name")
    private String stockName;
    private String sector;

    @Column(name = "market_cap_amount")
    private Double marketCapAmount;
    private Integer volume;

    @Column(name = "sizzle_index")
    private Double sizzleIndex;

    private Double ask;
    private Double bid;

    @Column(name = "number_of_share")
    private Integer numberOfShares;

    @Column(name = "open_price")
    private Double open;

    @Column(name = "close_price")
    private Double close;

    @Column(name = "last_price")
    private static Double last;

    @Column(name = "mark_change")
    private Double markChange;

    @ElementCollection
    @CollectionTable(name = "stock_historical_data", joinColumns = @JoinColumn(name = "stock_stockTicker"))
    @Column(name = "price")
    private List<Double> historicalData;

    @OneToMany(mappedBy = "stocks")
    private List<Position> positions;

    @OneToMany(mappedBy = "stocks")
    private List<Trade> trades;

    @ManyToOne
    private OptionTradeDetails optionTradeDetails;

    @ManyToMany
    private List<Watchlist> watchlists;

    public List<Double> getHistoricalData() {
        return historicalData;
    }

    public void setHistoricalData(List<Double> historicalData) {
        this.historicalData = historicalData;
    }

    public Double getMarkChange() {
        return markChange;
    }

    public void setMarkChange(Double markChange) {
        this.markChange = markChange;
    }

    public static Double getLast( ) {
        return last;
    }

    public void setLast(Double last) {
        this.last = last;
    }

    public Double getClose() {
        return close;
    }

    public void setClose(Double close) {
        this.close = close;
    }

    public Double getOpen() {
        return open;
    }

    public void setOpen(Double open) {
        this.open = open;
    }

    public Integer getNumberOfShares() {
        return numberOfShares;
    }

    public void setNumberOfShares(Integer numberOfShares) {
        this.numberOfShares = numberOfShares;
    }

    public Double getBid() {
        return bid;
    }

    public void setBid(Double bid) {
        this.bid = bid;
    }

    public Double getAsk() {
        return ask;
    }

    public void setAsk(Double ask) {
        this.ask = ask;
    }

    public Double getSizzleIndex() {
        return sizzleIndex;
    }

    public void setSizzleIndex(Double sizzleIndex) {
        this.sizzleIndex = sizzleIndex;
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public void setTrades(List<Trade> trades) {
        this.trades = trades;
    }

    public Stock() {}
    // Constructor
//    public Stock(String stockTicker) {
//        this.stockTicker = stockTicker;
//    }

    public Stock(String stockTicker, String stockName, String sector, Double marketCapAmount, Integer volume, Double sizzleIndex,
                Double ask, Double bid, Integer numberOfShares, Double open, Double close, Double last, Double markChange) {

        this.stockTicker = stockTicker;
        this.stockName = stockName;
        this.sector = sector;
        this.marketCapAmount = marketCapAmount;
        this.volume = volume;
        this.sizzleIndex = sizzleIndex;
        this.ask = ask;
        this.bid = bid;
        this.numberOfShares = numberOfShares;
        this.open = open;
        this.close = close;
        this.last = last;
        this.markChange = markChange;

    }
    // Getters and Setters
    public String getStockTicker() {
        return stockTicker;
    }

    public void setStockTicker(String stockTicker) {
        this.stockTicker = stockTicker;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public Double getMarketCapAmount() {
        return marketCapAmount;
    }

    public void setMarketCapAmount(Double marketCapAmount) {
        this.marketCapAmount = marketCapAmount;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    // Method to get Financial Statements (Stub for now)
//    public FinancialStatements getFinancialStatements(String stockTicker) {
//        // Replace this with real API/database calls
//        return new FinancialStatements(); // Example data
//    }
//
//    // Method to get Key Metrics (Stub for now)
//    public KeyMetrics getKeyMetrics(String stockTicker) {
//        // Replace this with real API/database calls
//        return new KeyMetrics(); // Example data
//    }

    // toString Method
    @Override
    public String toString() {
        return "Stock {" +
                "stockTicker='" + stockTicker + '\'' +
                ", stockName='" + stockName + '\'' +
                ", sector='" + sector + '\'' +
                ", marketCapAmount=" + marketCapAmount +
                ", volume=" + volume +
                ", sizzleIndex=" + sizzleIndex +
                ", ask=" + ask +
                ", bid=" + bid +
                ", numberOfShares=" + numberOfShares +
                ", open=" + open +
                ", close=" + close +
                ", last=" + last +
                ", markChange=" + markChange +
                '}';
    }
}
