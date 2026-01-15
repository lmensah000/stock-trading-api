package com.moneyteam.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

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
    @DisplayName("Should set and get user ID")
    void testSetAndGetUserId() {
        Long userId = 1L;
        position.setUserId(userId);
        assertEquals(userId, position.getUserId());
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
        BigDecimal avgPrice = BigDecimal.valueOf(378.91);
        position.setAveragePrice(avgPrice);
        assertEquals(avgPrice, position.getAveragePrice());
    }

    @Test
    @DisplayName("Should set and get unrealized P&L")
    void testSetAndGetUnrealizedPnL() {
        Double unrealizedPnL = 500.0;
        position.setUnrealizedPnL(unrealizedPnL);
        assertEquals(unrealizedPnL, position.getUnrealizedPnL());
    }

    @Test
    @DisplayName("Should calculate market value")
    void testCalculateMarketValue() {
        position.setTotalQuantity(10.0);
        BigDecimal currentPrice = BigDecimal.valueOf(400.0);
        
        BigDecimal marketValue = currentPrice.multiply(BigDecimal.valueOf(position.getTotalQuantity()));
        assertEquals(0, BigDecimal.valueOf(4000.0).compareTo(marketValue));
    }

    @Test
    @DisplayName("Should calculate cost basis")
    void testCalculateCostBasis() {
        position.setTotalQuantity(10.0);
        position.setAveragePrice(BigDecimal.valueOf(350.0));
        
        BigDecimal costBasis = position.getAveragePrice().multiply(BigDecimal.valueOf(position.getTotalQuantity()));
        assertEquals(0, BigDecimal.valueOf(3500.0).compareTo(costBasis));
    }

    @Test
    @DisplayName("Should calculate unrealized P&L")
    void testCalculateUnrealizedPnL() {
        position.setTotalQuantity(10.0);
        position.setAveragePrice(BigDecimal.valueOf(350.0));
        BigDecimal currentPrice = BigDecimal.valueOf(400.0);
        
        BigDecimal costBasis = position.getAveragePrice().multiply(BigDecimal.valueOf(position.getTotalQuantity()));
        BigDecimal marketValue = currentPrice.multiply(BigDecimal.valueOf(position.getTotalQuantity()));
        BigDecimal unrealizedPnL = marketValue.subtract(costBasis);
        
        assertEquals(0, BigDecimal.valueOf(500.0).compareTo(unrealizedPnL));
    }

    @Test
    @DisplayName("Should handle negative P&L")
    void testNegativePnL() {
        position.setTotalQuantity(10.0);
        position.setAveragePrice(BigDecimal.valueOf(100.0));
        BigDecimal currentPrice = BigDecimal.valueOf(90.0);
        
        BigDecimal costBasis = position.getAveragePrice().multiply(BigDecimal.valueOf(position.getTotalQuantity()));
        BigDecimal marketValue = currentPrice.multiply(BigDecimal.valueOf(position.getTotalQuantity()));
        BigDecimal unrealizedPnL = marketValue.subtract(costBasis);
        
        assertTrue(unrealizedPnL.compareTo(BigDecimal.ZERO) < 0);
        assertEquals(0, BigDecimal.valueOf(-100.0).compareTo(unrealizedPnL));
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

    @Test
    @DisplayName("Should set and get user ref ID")
    void testSetAndGetUserRefId() {
        Long userRefId = 1L;
        position.setUserRefId(userRefId);
        assertEquals(userRefId, position.getUserRefId());
    }
}
