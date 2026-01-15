package com.moneyteam.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for Watchlist model.
 */
@DisplayName("Watchlist Model Tests")
public class WatchlistTest {

    private Watchlist watchlist;

    @BeforeEach
    void setUp() {
        watchlist = new Watchlist();
    }

    @Test
    @DisplayName("Should create watchlist with default values")
    void testWatchlistCreation() {
        assertNotNull(watchlist);
    }

    @Test
    @DisplayName("Should set and get watchlist ID")
    void testSetAndGetId() {
        Long id = 1L;
        watchlist.setId(id);
        assertEquals(id, watchlist.getId());
    }

    @Test
    @DisplayName("Should set and get user ID")
    void testSetAndGetUserId() {
        Long userId = 1L;
        watchlist.setUserId(userId);
        assertEquals(userId, watchlist.getUserId());
    }

    @Test
    @DisplayName("Should set and get user reference")
    void testSetAndGetUser() {
        User user = new User();
        user.setId(1L);
        user.setUserName("testuser");
        watchlist.setUsers(user);
        
        assertNotNull(watchlist.getUsers());
        assertEquals("testuser", watchlist.getUsers().getUserName());
    }

    @Test
    @DisplayName("Should add stocks to watchlist")
    void testAddStocksToWatchlist() {
        List<Stock> stocks = new ArrayList<>();
        
        Stock aapl = new Stock();
        aapl.setStockTicker("AAPL");
        aapl.setStockName("Apple Inc.");
        
        Stock msft = new Stock();
        msft.setStockTicker("MSFT");
        msft.setStockName("Microsoft Corp.");
        
        stocks.add(aapl);
        stocks.add(msft);
        watchlist.setStocks(stocks);
        
        assertEquals(2, watchlist.getStocks().size());
    }

    @Test
    @DisplayName("Should check stock exists in watchlist")
    void testStockExistsInWatchlist() {
        List<Stock> stocks = new ArrayList<>();
        
        Stock aapl = new Stock();
        aapl.setStockTicker("AAPL");
        stocks.add(aapl);
        
        watchlist.setStocks(stocks);
        
        boolean exists = watchlist.getStocks().stream()
                .anyMatch(s -> "AAPL".equals(s.getStockTicker()));
        assertTrue(exists);
        
        boolean notExists = watchlist.getStocks().stream()
                .anyMatch(s -> "TSLA".equals(s.getStockTicker()));
        assertFalse(notExists);
    }

    @Test
    @DisplayName("Should handle empty watchlist")
    void testEmptyWatchlist() {
        List<Stock> stocks = new ArrayList<>();
        watchlist.setStocks(stocks);
        
        assertTrue(watchlist.getStocks().isEmpty());
        assertEquals(0, watchlist.getStocks().size());
    }

    @Test
    @DisplayName("Should remove stock from watchlist")
    void testRemoveStockFromWatchlist() {
        List<Stock> stocks = new ArrayList<>();
        
        Stock aapl = new Stock();
        aapl.setStockTicker("AAPL");
        
        Stock msft = new Stock();
        msft.setStockTicker("MSFT");
        
        stocks.add(aapl);
        stocks.add(msft);
        watchlist.setStocks(stocks);
        
        // Remove MSFT
        watchlist.getStocks().removeIf(s -> "MSFT".equals(s.getStockTicker()));
        
        assertEquals(1, watchlist.getStocks().size());
        boolean msfExists = watchlist.getStocks().stream()
                .anyMatch(s -> "MSFT".equals(s.getStockTicker()));
        assertFalse(msfExists);
    }

    @Test
    @DisplayName("Should get watchlist size")
    void testGetWatchlistSize() {
        List<Stock> stocks = new ArrayList<>();
        
        String[] tickers = {"AAPL", "MSFT", "GOOGL", "AMZN", "NVDA"};
        for (String ticker : tickers) {
            Stock stock = new Stock();
            stock.setStockTicker(ticker);
            stocks.add(stock);
        }
        
        watchlist.setStocks(stocks);
        assertEquals(5, watchlist.getStocks().size());
    }

    @Test
    @DisplayName("Should handle null stocks list")
    void testNullStocksList() {
        watchlist.setStocks(null);
        assertNull(watchlist.getStocks());
    }
}
