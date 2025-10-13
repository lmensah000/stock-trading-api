package com.moneyteam.model;

public class OptionsTradeRequest {
    private String stockTicker;
    private int quantity;
    private String tradeType; // "BUY" or "SELL"
    private Long userId;
}
