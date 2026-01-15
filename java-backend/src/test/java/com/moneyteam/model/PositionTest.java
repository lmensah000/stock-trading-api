package com.moneyteam.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Position model.
 */
@DisplayName("Position Model Tests")
public class PositionTest {

    private Position position;

    @BeforeEach
    void setUp() {
        position = new Position();
    }

    @Test
    @DisplayName("Should create position with default values")
    void testPositionCreation() {
        assertNotNull(position);
    }

    @Test
    @DisplayName("Should set and get position ID")
    void testSetAndGetId() {
        Long id = 1L;
        position.setId(id);
        assertEquals(id, position.getId());
    }

    @Test
    @DisplayName("Should set and get stock ticker")
    void testSetAndGetStockTicker() {
        String ticker = "MSFT";
        position.setStockTicker(ticker);
        assertEquals(ticker, position.getStockTicker());
    }

    @Test
    @DisplayName("Should set and get total quantity")
    void testSetAndGetTotalQuantity() {
        Double quantity = 100.0;
        position.setTotalQuantity(quantity);
        assertEquals(quantity, position.getTotalQuantity());
    }

    @Test
    @DisplayName("Should set and get average price")
    void testSetAndGetAveragePrice() {
        Double avgPrice = 378.91;
        position.setAveragePrice(avgPrice);
        assertEquals(avgPrice, position.getAveragePrice());
    }

    @Test
    @DisplayName("Should calculate market value")
    void testCalculateMarketValue() {
        position.setTotalQuantity(10.0);
        Double currentPrice = 400.0;
        
        Double marketValue = position.getTotalQuantity() * currentPrice;
        assertEquals(4000.0, marketValue);
    }

    @Test
    @DisplayName("Should calculate cost basis")
    void testCalculateCostBasis() {
        position.setTotalQuantity(10.0);
        position.setAveragePrice(350.0);
        
        Double costBasis = position.getTotalQuantity() * position.getAveragePrice();
        assertEquals(3500.0, costBasis);
    }

    @Test
    @DisplayName("Should calculate unrealized P&L")
    void testCalculateUnrealizedPnL() {
        position.setTotalQuantity(10.0);
        position.setAveragePrice(350.0);
        Double currentPrice = 400.0;
        
        Double costBasis = position.getTotalQuantity() * position.getAveragePrice();
        Double marketValue = position.getTotalQuantity() * currentPrice;
        Double unrealizedPnL = marketValue - costBasis;
        
        assertEquals(500.0, unrealizedPnL);
    }

    @Test
    @DisplayName("Should calculate unrealized P&L percentage")
    void testCalculateUnrealizedPnLPercent() {
        position.setTotalQuantity(10.0);
        position.setAveragePrice(100.0);
        Double currentPrice = 110.0;
        
        Double costBasis = position.getTotalQuantity() * position.getAveragePrice();
        Double marketValue = position.getTotalQuantity() * currentPrice;
        Double pnlPercent = ((marketValue - costBasis) / costBasis) * 100;
        
        assertEquals(10.0, pnlPercent);
    }

    @Test
    @DisplayName("Should handle negative P&L")
    void testNegativePnL() {
        position.setTotalQuantity(10.0);
        position.setAveragePrice(100.0);
        Double currentPrice = 90.0;
        
        Double costBasis = position.getTotalQuantity() * position.getAveragePrice();
        Double marketValue = position.getTotalQuantity() * currentPrice;
        Double unrealizedPnL = marketValue - costBasis;
        
        assertTrue(unrealizedPnL < 0);
        assertEquals(-100.0, unrealizedPnL);
    }

    @Test
    @DisplayName("Should set and get user reference")
    void testSetAndGetUser() {
        User user = new User();
        user.setId(1L);
        position.setUsers(user);
        
        assertNotNull(position.getUsers());
        assertEquals(1L, position.getUsers().getId());
    }
}
