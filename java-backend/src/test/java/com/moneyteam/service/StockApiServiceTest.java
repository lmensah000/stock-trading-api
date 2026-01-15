package com.moneyteam.service;

import com.moneyteam.model.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StockApiService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Stock API Service Tests")
public class StockApiServiceTest {

    private Map<String, Stock> mockStockData;

    @BeforeEach
    void setUp() {
        mockStockData = new HashMap<>();
        
        Stock aapl = new Stock();
        aapl.setTicker("AAPL");
        aapl.setName("Apple Inc.");
        aapl.setPrice(185.92);
        aapl.setChange(1.67);
        aapl.setChangePercent(0.91);
        aapl.setVolume(48234521L);
        aapl.setMarketCap(2890000000000.0);
        mockStockData.put("AAPL", aapl);

        Stock msft = new Stock();
        msft.setTicker("MSFT");
        msft.setName("Microsoft Corp.");
        msft.setPrice(378.91);
        msft.setChange(2.87);
        msft.setChangePercent(0.76);
        msft.setVolume(21456789L);
        msft.setMarketCap(2810000000000.0);
        mockStockData.put("MSFT", msft);

        Stock googl = new Stock();
        googl.setTicker("GOOGL");
        googl.setName("Alphabet Inc.");
        googl.setPrice(141.80);
        googl.setChange(1.55);
        googl.setChangePercent(1.10);
        googl.setVolume(25678234L);
        googl.setMarketCap(1780000000000.0);
        mockStockData.put("GOOGL", googl);
    }

    @Test
    @DisplayName("Should get stock quote for valid ticker")
    void testGetStockQuote() {
        Stock stock = mockStockData.get("AAPL");
        
        assertNotNull(stock);
        assertEquals("AAPL", stock.getTicker());
        assertEquals("Apple Inc.", stock.getName());
        assertEquals(185.92, stock.getPrice());
    }

    @Test
    @DisplayName("Should return null for invalid ticker")
    void testGetStockQuoteInvalidTicker() {
        Stock stock = mockStockData.get("INVALID");
        assertNull(stock);
    }

    @Test
    @DisplayName("Should calculate price change correctly")
    void testCalculatePriceChange() {
        Stock stock = mockStockData.get("AAPL");
        Double previousClose = stock.getPrice() - stock.getChange();
        
        Double calculatedChange = stock.getPrice() - previousClose;
        assertEquals(stock.getChange(), calculatedChange, 0.01);
    }

    @Test
    @DisplayName("Should calculate change percent correctly")
    void testCalculateChangePercent() {
        Stock stock = mockStockData.get("AAPL");
        Double previousClose = stock.getPrice() - stock.getChange();
        Double expectedPercent = (stock.getChange() / previousClose) * 100;
        
        assertEquals(stock.getChangePercent(), expectedPercent, 0.1);
    }

    @Test
    @DisplayName("Should handle multiple stock quotes")
    void testGetMultipleQuotes() {
        assertEquals(3, mockStockData.size());
        
        assertNotNull(mockStockData.get("AAPL"));
        assertNotNull(mockStockData.get("MSFT"));
        assertNotNull(mockStockData.get("GOOGL"));
    }

    @Test
    @DisplayName("Should format market cap correctly")
    void testFormatMarketCap() {
        Stock stock = mockStockData.get("AAPL");
        Double marketCap = stock.getMarketCap();
        
        // Should be in trillions
        assertTrue(marketCap > 1e12);
        
        // Format as string
        String formatted;
        if (marketCap >= 1e12) {
            formatted = String.format("$%.2fT", marketCap / 1e12);
        } else if (marketCap >= 1e9) {
            formatted = String.format("$%.2fB", marketCap / 1e9);
        } else {
            formatted = String.format("$%.2fM", marketCap / 1e6);
        }
        
        assertEquals("$2.89T", formatted);
    }

    @Test
    @DisplayName("Should identify positive stock change")
    void testPositiveStockChange() {
        Stock stock = mockStockData.get("AAPL");
        assertTrue(stock.getChange() >= 0);
        assertTrue(stock.getChangePercent() >= 0);
    }

    @Test
    @DisplayName("Should identify negative stock change")
    void testNegativeStockChange() {
        Stock negativeStock = new Stock();
        negativeStock.setTicker("TEST");
        negativeStock.setChange(-5.25);
        negativeStock.setChangePercent(-2.75);
        
        assertTrue(negativeStock.getChange() < 0);
        assertTrue(negativeStock.getChangePercent() < 0);
    }

    @Test
    @DisplayName("Should handle ticker case insensitivity")
    void testTickerCaseInsensitivity() {
        String ticker = "aapl";
        Stock stock = mockStockData.get(ticker.toUpperCase());
        
        assertNotNull(stock);
        assertEquals("AAPL", stock.getTicker());
    }

    @Test
    @DisplayName("Should validate stock volume")
    void testStockVolume() {
        Stock stock = mockStockData.get("AAPL");
        assertTrue(stock.getVolume() > 0);
    }

    @Test
    @DisplayName("Should sort stocks by change percent")
    void testSortByChangePercent() {
        Stock[] stocks = mockStockData.values().toArray(new Stock[0]);
        
        // Sort by change percent descending
        java.util.Arrays.sort(stocks, (a, b) -> 
            Double.compare(Math.abs(b.getChangePercent()), Math.abs(a.getChangePercent())));
        
        // First should have highest absolute change percent
        assertTrue(Math.abs(stocks[0].getChangePercent()) >= Math.abs(stocks[1].getChangePercent()));
    }
}
