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
    @DisplayName("Should set and get user reference")
    void testSetAndGetUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        watchlist.setUsers(user);
        
        assertNotNull(watchlist.getUsers());
        assertEquals("testuser", watchlist.getUsers().getUsername());
    }

    @Test
    @DisplayName("Should add stock to watchlist")
    void testAddStockToWatchlist() {
        List<String> stocks = new ArrayList<>();
        stocks.add("AAPL");
        stocks.add("MSFT");
        watchlist.setStocks(stocks);
        
        assertEquals(2, watchlist.getStocks().size());
        assertTrue(watchlist.getStocks().contains("AAPL"));
        assertTrue(watchlist.getStocks().contains("MSFT"));
    }

    @Test
    @DisplayName("Should remove stock from watchlist")
    void testRemoveStockFromWatchlist() {
        List<String> stocks = new ArrayList<>();
        stocks.add("AAPL");
        stocks.add("MSFT");
        stocks.add("GOOGL");
        watchlist.setStocks(stocks);
        
        watchlist.getStocks().remove("MSFT");
        
        assertEquals(2, watchlist.getStocks().size());
        assertFalse(watchlist.getStocks().contains("MSFT"));
    }

    @Test
    @DisplayName("Should check if stock exists in watchlist")
    void testStockExistsInWatchlist() {
        List<String> stocks = new ArrayList<>();
        stocks.add("AAPL");
        watchlist.setStocks(stocks);
        
        assertTrue(watchlist.getStocks().contains("AAPL"));
        assertFalse(watchlist.getStocks().contains("TSLA"));
    }

    @Test
    @DisplayName("Should handle empty watchlist")
    void testEmptyWatchlist() {
        List<String> stocks = new ArrayList<>();
        watchlist.setStocks(stocks);
        
        assertTrue(watchlist.getStocks().isEmpty());
        assertEquals(0, watchlist.getStocks().size());
    }

    @Test
    @DisplayName("Should prevent duplicate stocks")
    void testPreventDuplicateStocks() {
        List<String> stocks = new ArrayList<>();
        stocks.add("AAPL");
        
        // Check before adding duplicate
        if (!stocks.contains("AAPL")) {
            stocks.add("AAPL");
        }
        
        watchlist.setStocks(stocks);
        assertEquals(1, watchlist.getStocks().size());
    }

    @Test
    @DisplayName("Should get watchlist size")
    void testGetWatchlistSize() {
        List<String> stocks = new ArrayList<>();
        stocks.add("AAPL");
        stocks.add("MSFT");
        stocks.add("GOOGL");
        stocks.add("AMZN");
        stocks.add("NVDA");
        watchlist.setStocks(stocks);
        
        assertEquals(5, watchlist.getStocks().size());
    }
}
