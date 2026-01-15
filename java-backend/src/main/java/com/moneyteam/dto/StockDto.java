package com.moneyteam.dto;

import java.util.List;

/**
 * Data Transfer Object for Stock entity.
 */
public class StockDto {

    private String stockTicker;
    private String stockName;
    private String sector;
    private Double marketCapAmount;
    private Integer volume;
    private Double sizzleIndex;
    private Double ask;
    private Double bid;
    private Integer numberOfShares;
    private Double open;
    private Double close;
    private Double last;
    private Double markChange;
    private Double changePercent;
    private List<Double> historicalData;

    public StockDto() {}

    public StockDto(String stockTicker, String stockName, Double last, Double markChange) {
        this.stockTicker = stockTicker;
        this.stockName = stockName;
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

    public Double getSizzleIndex() {
        return sizzleIndex;
    }

    public void setSizzleIndex(Double sizzleIndex) {
        this.sizzleIndex = sizzleIndex;
    }

    public Double getAsk() {
        return ask;
    }

    public void setAsk(Double ask) {
        this.ask = ask;
    }

    public Double getBid() {
        return bid;
    }

    public void setBid(Double bid) {
        this.bid = bid;
    }

    public Integer getNumberOfShares() {
        return numberOfShares;
    }

    public void setNumberOfShares(Integer numberOfShares) {
        this.numberOfShares = numberOfShares;
    }

    public Double getOpen() {
        return open;
    }

    public void setOpen(Double open) {
        this.open = open;
    }

    public Double getClose() {
        return close;
    }

    public void setClose(Double close) {
        this.close = close;
    }

    public Double getLast() {
        return last;
    }

    public void setLast(Double last) {
        this.last = last;
    }

    public Double getMarkChange() {
        return markChange;
    }

    public void setMarkChange(Double markChange) {
        this.markChange = markChange;
    }

    public Double getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(Double changePercent) {
        this.changePercent = changePercent;
    }

    public List<Double> getHistoricalData() {
        return historicalData;
    }

    public void setHistoricalData(List<Double> historicalData) {
        this.historicalData = historicalData;
    }

    @Override
    public String toString() {
        return "StockDto{" +
                "stockTicker='" + stockTicker + '\'' +
                ", stockName='" + stockName + '\'' +
                ", last=" + last +
                ", markChange=" + markChange +
                ", changePercent=" + changePercent +
                '}';
    }
}
