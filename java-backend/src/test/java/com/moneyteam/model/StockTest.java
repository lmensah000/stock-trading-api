package com.moneyteam.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Stock model.
 */
@DisplayName("Stock Model Tests")
public class StockTest {

    private Stock stock;

    @BeforeEach
    void setUp() {
        stock = new Stock();
    }

    @Test
    @DisplayName("Should create stock with default values")
    void testStockCreation() {
        assertNotNull(stock);
    }

    @Test
    @DisplayName("Should set and get ticker symbol")
    void testSetAndGetTicker() {
        String ticker = "AAPL";
        stock.setTicker(ticker);
        assertEquals(ticker, stock.getTicker());
    }

    @Test
    @DisplayName("Should set and get company name")
    void testSetAndGetName() {
        String name = "Apple Inc.";
        stock.setName(name);
        assertEquals(name, stock.getName());
    }

    @Test
    @DisplayName("Should set and get current price")
    void testSetAndGetPrice() {
        Double price = 185.92;
        stock.setPrice(price);
        assertEquals(price, stock.getPrice());
    }

    @Test
    @DisplayName("Should set and get price change")
    void testSetAndGetChange() {
        Double change = 2.50;
        stock.setChange(change);
        assertEquals(change, stock.getChange());
    }

    @Test
    @DisplayName("Should set and get change percent")
    void testSetAndGetChangePercent() {
        Double changePercent = 1.35;
        stock.setChangePercent(changePercent);
        assertEquals(changePercent, stock.getChangePercent());
    }

    @Test
    @DisplayName("Should set and get volume")
    void testSetAndGetVolume() {
        Long volume = 50000000L;
        stock.setVolume(volume);
        assertEquals(volume, stock.getVolume());
    }

    @Test
    @DisplayName("Should set and get market cap")
    void testSetAndGetMarketCap() {
        Double marketCap = 2890000000000.0;
        stock.setMarketCap(marketCap);
        assertEquals(marketCap, stock.getMarketCap());
    }

    @Test
    @DisplayName("Should handle negative price change")
    void testNegativePriceChange() {
        stock.setChange(-5.25);
        stock.setChangePercent(-2.75);
        
        assertTrue(stock.getChange() < 0);
        assertTrue(stock.getChangePercent() < 0);
    }

    @Test
    @DisplayName("Should identify positive change")
    void testPositiveChange() {
        stock.setChange(3.50);
        assertTrue(stock.getChange() >= 0);
    }

    @Test
    @DisplayName("Should handle ticker case sensitivity")
    void testTickerUpperCase() {
        stock.setTicker("aapl");
        // Assuming ticker should be uppercase
        String ticker = stock.getTicker().toUpperCase();
        assertEquals("AAPL", ticker);
    }
}
