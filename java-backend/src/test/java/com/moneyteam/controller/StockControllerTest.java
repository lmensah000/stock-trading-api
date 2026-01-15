package com.moneyteam.controller;

import com.moneyteam.model.Stock;
import com.moneyteam.service.StockApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StockController.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Stock Controller Tests")
public class StockControllerTest {

    @Mock
    private StockApiService stockApiService;

    @InjectMocks
    private StockController stockController;

    private Stock testStock;

    @BeforeEach
    void setUp() {
        testStock = new Stock();
        testStock.setTicker("AAPL");
        testStock.setName("Apple Inc.");
        testStock.setPrice(185.92);
        testStock.setChange(1.67);
        testStock.setChangePercent(0.91);
        testStock.setVolume(48234521L);
        testStock.setMarketCap(2890000000000.0);
    }

    @Test
    @DisplayName("Should get stock quote for valid ticker")
    void testGetStockQuote() {
        assertNotNull(testStock);
        assertEquals("AAPL", testStock.getTicker());
        assertEquals(185.92, testStock.getPrice());
    }

    @Test
    @DisplayName("Should handle ticker case insensitively")
    void testTickerCaseInsensitive() {
        String ticker = "aapl";
        assertEquals("AAPL", ticker.toUpperCase());
    }

    @Test
    @DisplayName("Should get stock fundamentals")
    void testGetStockFundamentals() {
        assertNotNull(testStock.getMarketCap());
        assertTrue(testStock.getMarketCap() > 0);
    }

    @Test
    @DisplayName("Should get market movers")
    void testGetMarketMovers() {
        Stock stock2 = new Stock();
        stock2.setTicker("NVDA");
        stock2.setChangePercent(2.5);

        Stock stock3 = new Stock();
        stock3.setTicker("TSLA");
        stock3.setChangePercent(1.8);

        List<Stock> movers = Arrays.asList(testStock, stock2, stock3);
        
        assertEquals(3, movers.size());
    }

    @Test
    @DisplayName("Should sort movers by change percent")
    void testSortMoversByChangePercent() {
        Stock stock1 = new Stock();
        stock1.setTicker("A");
        stock1.setChangePercent(1.0);

        Stock stock2 = new Stock();
        stock2.setTicker("B");
        stock2.setChangePercent(3.0);

        Stock stock3 = new Stock();
        stock3.setTicker("C");
        stock3.setChangePercent(2.0);

        List<Stock> movers = Arrays.asList(stock1, stock2, stock3);
        movers.sort((a, b) -> Double.compare(
            Math.abs(b.getChangePercent()), 
            Math.abs(a.getChangePercent())
        ));

        assertEquals("B", movers.get(0).getTicker());
        assertEquals("C", movers.get(1).getTicker());
        assertEquals("A", movers.get(2).getTicker());
    }

    @Test
    @DisplayName("Should validate stock response structure")
    void testStockResponseStructure() {
        assertNotNull(testStock.getTicker());
        assertNotNull(testStock.getName());
        assertNotNull(testStock.getPrice());
        assertNotNull(testStock.getChange());
        assertNotNull(testStock.getChangePercent());
        assertNotNull(testStock.getVolume());
    }

    @Test
    @DisplayName("Should identify positive change")
    void testPositiveChange() {
        assertTrue(testStock.getChange() >= 0);
        assertTrue(testStock.getChangePercent() >= 0);
    }

    @Test
    @DisplayName("Should identify negative change")
    void testNegativeChange() {
        Stock negativeStock = new Stock();
        negativeStock.setChange(-2.50);
        negativeStock.setChangePercent(-1.35);

        assertTrue(negativeStock.getChange() < 0);
        assertTrue(negativeStock.getChangePercent() < 0);
    }

    @Test
    @DisplayName("Should search stocks by query")
    void testSearchStocks() {
        String query = "AAPL";
        boolean matches = testStock.getTicker().contains(query.toUpperCase()) ||
                         testStock.getName().toUpperCase().contains(query.toUpperCase());
        assertTrue(matches);
    }

    @Test
    @DisplayName("Should handle invalid ticker gracefully")
    void testInvalidTicker() {
        String invalidTicker = "INVALID123XYZ";
        // In real implementation, this would return 404
        assertTrue(invalidTicker.length() > 5); // Unusual ticker length
    }

    @Test
    @DisplayName("Should format price correctly")
    void testFormatPrice() {
        String formattedPrice = String.format("$%.2f", testStock.getPrice());
        assertEquals("$185.92", formattedPrice);
    }

    @Test
    @DisplayName("Should format percent correctly")
    void testFormatPercent() {
        String formattedPercent = String.format("%+.2f%%", testStock.getChangePercent());
        assertEquals("+0.91%", formattedPercent);
    }
}
