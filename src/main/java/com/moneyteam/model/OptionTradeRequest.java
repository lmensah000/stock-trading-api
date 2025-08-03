package com.moneyteam.model;

public class OptionTradeRequest {
    private String stockTicker;
    private int quantity;
    private String tradeType; // "BUY" or "SELL"
    private Long userId;
}
