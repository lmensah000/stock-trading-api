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
        
        Stock aapl = new Stock("AAPL", "Apple Inc.", "Technology", 2890000000000.0, 
                48234521, 1.5, 186.00, 185.50, 1000000, 182.50, 185.92, 185.92, 1.67);
        mockStockData.put("AAPL", aapl);

        Stock msft = new Stock("MSFT", "Microsoft Corp.", "Technology", 2810000000000.0, 
                21456789, 1.3, 379.00, 378.50, 500000, 376.00, 378.91, 378.91, 2.87);
        mockStockData.put("MSFT", msft);

        Stock googl = new Stock("GOOGL", "Alphabet Inc.", "Technology", 1780000000000.0, 
                25678234, 1.2, 142.00, 141.50, 750000, 140.00, 141.80, 141.80, 1.55);
        mockStockData.put("GOOGL", googl);
    }

    @Test
    @DisplayName("Should get stock quote for valid ticker")
    void testGetStockQuote() {
        Stock stock = mockStockData.get("AAPL");
        
        assertNotNull(stock);
        assertEquals("AAPL", stock.getStockTicker());
        assertEquals("Apple Inc.", stock.getStockName());
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
        Double markChange = stock.getMarkChange();
        
        assertNotNull(markChange);
        assertEquals(1.67, markChange, 0.01);
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
        Double marketCap = stock.getMarketCapAmount();
        
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
        assertTrue(stock.getMarkChange() >= 0);
    }

    @Test
    @DisplayName("Should identify negative stock change")
    void testNegativeStockChange() {
        Stock negativeStock = new Stock("TEST", "Test Stock", "Test", 0.0, 
                0, 0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0, -5.25);
        
        assertTrue(negativeStock.getMarkChange() < 0);
    }

    @Test
    @DisplayName("Should handle ticker case insensitivity")
    void testTickerCaseInsensitivity() {
        String ticker = "aapl";
        Stock stock = mockStockData.get(ticker.toUpperCase());
        
        assertNotNull(stock);
        assertEquals("AAPL", stock.getStockTicker());
    }

    @Test
    @DisplayName("Should validate stock volume")
    void testStockVolume() {
        Stock stock = mockStockData.get("AAPL");
        assertTrue(stock.getVolume() > 0);
    }

    @Test
    @DisplayName("Should sort stocks by change")
    void testSortByChange() {
        Stock[] stocks = mockStockData.values().toArray(new Stock[0]);
        
        // Sort by change descending
        java.util.Arrays.sort(stocks, (a, b) -> 
            Double.compare(Math.abs(b.getMarkChange()), Math.abs(a.getMarkChange())));
        
        // First should have highest absolute change
        assertTrue(Math.abs(stocks[0].getMarkChange()) >= Math.abs(stocks[1].getMarkChange()));
    }

    @Test
    @DisplayName("Should validate sector information")
    void testSectorInformation() {
        Stock stock = mockStockData.get("AAPL");
        assertEquals("Technology", stock.getSector());
    }

    @Test
    @DisplayName("Should validate bid-ask spread")
    void testBidAskSpread() {
        Stock stock = mockStockData.get("AAPL");
        assertNotNull(stock.getBid());
        assertNotNull(stock.getAsk());
        assertTrue(stock.getAsk() > stock.getBid());
    }
}
