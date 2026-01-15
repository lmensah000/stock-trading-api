package com.moneyteam.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

/**
 * Unit tests for Trade model.
 */
@DisplayName("Trade Model Tests")
public class TradeTest {

    private Trade trade;

    @BeforeEach
    void setUp() {
        trade = new Trade();
    }

    @Test
    @DisplayName("Should create trade with default values")
    void testTradeCreation() {
        assertNotNull(trade);
    }

    @Test
    @DisplayName("Should set and get trade ID")
    void testSetAndGetId() {
        Long id = 100L;
        trade.setId(id);
        assertEquals(id, trade.getId());
    }

    @Test
    @DisplayName("Should set and get stock ticker")
    void testSetAndGetStockTicker() {
        String ticker = "AAPL";
        trade.setStockTicker(ticker);
        assertEquals(ticker, trade.getStockTicker());
    }

    @Test
    @DisplayName("Should set and get quantity")
    void testSetAndGetQuantity() {
        Double quantity = 10.5;
        trade.setQuantity(quantity);
        assertEquals(quantity, trade.getQuantity());
    }

    @Test
    @DisplayName("Should set and get price")
    void testSetAndGetPrice() {
        Double price = 185.92;
        trade.setPrice(price);
        assertEquals(price, trade.getPrice());
    }

    @Test
    @DisplayName("Should set and get trade type BUY")
    void testSetAndGetTradeTypeBuy() {
        String tradeType = "BUY";
        trade.setTradeType(tradeType);
        assertEquals(tradeType, trade.getTradeType());
    }

    @Test
    @DisplayName("Should set and get trade type SELL")
    void testSetAndGetTradeTypeSell() {
        String tradeType = "SELL";
        trade.setTradeType(tradeType);
        assertEquals(tradeType, trade.getTradeType());
    }

    @Test
    @DisplayName("Should set and get status")
    void testSetAndGetStatus() {
        String status = "EXECUTED";
        trade.setStatus(status);
        assertEquals(status, trade.getStatus());
    }

    @Test
    @DisplayName("Should set and get execution date")
    void testSetAndGetExecutionDate() {
        LocalDateTime now = LocalDateTime.now();
        trade.setExecutionDate(now);
        assertEquals(now, trade.getExecutionDate());
    }

    @Test
    @DisplayName("Should calculate total value correctly")
    void testCalculateTotalValue() {
        trade.setQuantity(10.0);
        trade.setPrice(100.0);
        
        Double expectedTotal = 10.0 * 100.0;
        Double actualTotal = trade.getQuantity() * trade.getPrice();
        
        assertEquals(expectedTotal, actualTotal);
    }

    @Test
    @DisplayName("Should handle zero quantity")
    void testZeroQuantity() {
        trade.setQuantity(0.0);
        assertEquals(0.0, trade.getQuantity());
    }

    @Test
    @DisplayName("Should handle fractional shares")
    void testFractionalShares() {
        trade.setQuantity(0.5);
        trade.setPrice(200.0);
        
        Double total = trade.getQuantity() * trade.getPrice();
        assertEquals(100.0, total);
    }
}
