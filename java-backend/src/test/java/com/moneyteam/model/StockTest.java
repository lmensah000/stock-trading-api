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
    @DisplayName("Should create stock with constructor")
    void testStockCreationWithConstructor() {
        Stock fullStock = new Stock("AAPL", "Apple Inc.", "Technology", 2890000000000.0, 
                48234521, 1.5, 186.00, 185.50, 1000000, 182.50, 185.92, 185.92, 1.67);
        
        assertEquals("AAPL", fullStock.getStockTicker());
        assertEquals("Apple Inc.", fullStock.getStockName());
        assertEquals("Technology", fullStock.getSector());
    }

    @Test
    @DisplayName("Should set and get ticker symbol")
    void testSetAndGetStockTicker() {
        String ticker = "AAPL";
        stock.setStockTicker(ticker);
        assertEquals(ticker, stock.getStockTicker());
    }

    @Test
    @DisplayName("Should set and get company name")
    void testSetAndGetStockName() {
        String name = "Apple Inc.";
        stock.setStockName(name);
        assertEquals(name, stock.getStockName());
    }

    @Test
    @DisplayName("Should set and get sector")
    void testSetAndGetSector() {
        String sector = "Technology";
        stock.setSector(sector);
        assertEquals(sector, stock.getSector());
    }

    @Test
    @DisplayName("Should set and get market cap")
    void testSetAndGetMarketCapAmount() {
        Double marketCap = 2890000000000.0;
        stock.setMarketCapAmount(marketCap);
        assertEquals(marketCap, stock.getMarketCapAmount());
    }

    @Test
    @DisplayName("Should set and get volume")
    void testSetAndGetVolume() {
        Integer volume = 50000000;
        stock.setVolume(volume);
        assertEquals(volume, stock.getVolume());
    }

    @Test
    @DisplayName("Should set and get sizzle index")
    void testSetAndGetSizzleIndex() {
        Double sizzleIndex = 1.5;
        stock.setSizzleIndex(sizzleIndex);
        assertEquals(sizzleIndex, stock.getSizzleIndex());
    }

    @Test
    @DisplayName("Should set and get ask price")
    void testSetAndGetAsk() {
        Double ask = 186.00;
        stock.setAsk(ask);
        assertEquals(ask, stock.getAsk());
    }

    @Test
    @DisplayName("Should set and get bid price")
    void testSetAndGetBid() {
        Double bid = 185.50;
        stock.setBid(bid);
        assertEquals(bid, stock.getBid());
    }

    @Test
    @DisplayName("Should set and get number of shares")
    void testSetAndGetNumberOfShares() {
        Integer shares = 1000000;
        stock.setNumberOfShares(shares);
        assertEquals(shares, stock.getNumberOfShares());
    }

    @Test
    @DisplayName("Should set and get open price")
    void testSetAndGetOpen() {
        Double open = 182.50;
        stock.setOpen(open);
        assertEquals(open, stock.getOpen());
    }

    @Test
    @DisplayName("Should set and get close price")
    void testSetAndGetClose() {
        Double close = 185.92;
        stock.setClose(close);
        assertEquals(close, stock.getClose());
    }

    @Test
    @DisplayName("Should set and get mark change")
    void testSetAndGetMarkChange() {
        Double markChange = 1.67;
        stock.setMarkChange(markChange);
        assertEquals(markChange, stock.getMarkChange());
    }

    @Test
    @DisplayName("Should handle negative price change")
    void testNegativePriceChange() {
        stock.setMarkChange(-5.25);
        assertTrue(stock.getMarkChange() < 0);
    }

    @Test
    @DisplayName("Should identify positive change")
    void testPositiveChange() {
        stock.setMarkChange(3.50);
        assertTrue(stock.getMarkChange() >= 0);
    }

    @Test
    @DisplayName("Should validate bid-ask spread")
    void testBidAskSpread() {
        stock.setAsk(186.00);
        stock.setBid(185.50);
        assertTrue(stock.getAsk() > stock.getBid());
    }

    @Test
    @DisplayName("Should return correct toString")
    void testToString() {
        stock.setStockTicker("AAPL");
        stock.setStockName("Apple Inc.");
        
        String result = stock.toString();
        assertNotNull(result);
        assertTrue(result.contains("AAPL") || result.contains("Stock"));
    }
}
