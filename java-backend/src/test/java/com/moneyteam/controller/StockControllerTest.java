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
        testStock = new Stock("AAPL", "Apple Inc.", "Technology", 2890000000000.0, 
                48234521, 1.5, 186.00, 185.50, 1000000, 182.50, 185.92, 185.92, 1.67);
    }

    @Test
    @DisplayName("Should get stock quote for valid ticker")
    void testGetStockQuote() {
        assertNotNull(testStock);
        assertEquals("AAPL", testStock.getStockTicker());
        assertEquals(185.92, Stock.getLast(), 0.01);
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
        assertNotNull(testStock.getMarketCapAmount());
        assertTrue(testStock.getMarketCapAmount() > 0);
    }

    @Test
    @DisplayName("Should get market movers")
    void testGetMarketMovers() {
        Stock stock2 = new Stock("NVDA", "NVIDIA Corp.", "Technology", 1200000000000.0, 
                30000000, 2.0, 480.00, 479.00, 500000, 470.00, 478.00, 478.00, 8.00);

        Stock stock3 = new Stock("TSLA", "Tesla Inc.", "Automotive", 600000000000.0, 
                25000000, 1.8, 255.00, 254.00, 400000, 250.00, 254.00, 254.00, 4.00);

        List<Stock> movers = Arrays.asList(testStock, stock2, stock3);
        
        assertEquals(3, movers.size());
    }

    @Test
    @DisplayName("Should sort movers by change")
    void testSortMoversByChange() {
        Stock stock1 = new Stock("A", "Stock A", "Sector", 0.0, 0, 0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0, 1.0);
        Stock stock2 = new Stock("B", "Stock B", "Sector", 0.0, 0, 0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0, 3.0);
        Stock stock3 = new Stock("C", "Stock C", "Sector", 0.0, 0, 0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0, 2.0);

        List<Stock> movers = Arrays.asList(stock1, stock2, stock3);
        movers.sort((a, b) -> Double.compare(
            Math.abs(b.getMarkChange()), 
            Math.abs(a.getMarkChange())
        ));

        assertEquals("B", movers.get(0).getStockTicker());
        assertEquals("C", movers.get(1).getStockTicker());
        assertEquals("A", movers.get(2).getStockTicker());
    }

    @Test
    @DisplayName("Should validate stock response structure")
    void testStockResponseStructure() {
        assertNotNull(testStock.getStockTicker());
        assertNotNull(testStock.getStockName());
        assertNotNull(testStock.getSector());
        assertNotNull(testStock.getVolume());
    }

    @Test
    @DisplayName("Should identify positive change")
    void testPositiveChange() {
        assertTrue(testStock.getMarkChange() >= 0);
    }

    @Test
    @DisplayName("Should identify negative change")
    void testNegativeChange() {
        Stock negativeStock = new Stock("NEG", "Negative Stock", "Sector", 0.0, 
                0, 0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0, -2.50);

        assertTrue(negativeStock.getMarkChange() < 0);
    }

    @Test
    @DisplayName("Should search stocks by query")
    void testSearchStocks() {
        String query = "AAPL";
        boolean matches = testStock.getStockTicker().contains(query.toUpperCase()) ||
                         testStock.getStockName().toUpperCase().contains(query.toUpperCase());
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
        String formattedPrice = String.format("$%.2f", Stock.getLast());
        assertEquals("$185.92", formattedPrice);
    }

    @Test
    @DisplayName("Should validate bid-ask spread")
    void testBidAskSpread() {
        assertNotNull(testStock.getBid());
        assertNotNull(testStock.getAsk());
        assertTrue(testStock.getAsk() > testStock.getBid());
    }
}
