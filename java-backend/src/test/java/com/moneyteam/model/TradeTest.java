package com.moneyteam.model;

import com.moneyteam.model.enums.TradeStatus;
import com.moneyteam.model.enums.TradeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Unit tests for Trade model.
 */
@DisplayName("Trade Model Tests")
public class TradeTest {

    private Trade trade;

    @BeforeEach
    void setUp() {
        trade = new Trade();
    }

    @Test
    @DisplayName("Should create trade with default values")
    void testTradeCreation() {
        assertNotNull(trade);
    }

    @Test
    @DisplayName("Should set and get trade ID")
    void testSetAndGetId() {
        Long id = 100L;
        trade.setId(id);
        assertEquals(id, trade.getId());
    }

    @Test
    @DisplayName("Should set and get user trade ID")
    void testSetAndGetUserTradeId() {
        Long userId = 1L;
        trade.setUserTradeId(userId);
        assertEquals(userId, trade.getUserTradeId());
    }

    @Test
    @DisplayName("Should set and get stock ticker")
    void testSetAndGetStockTicker() {
        String ticker = "AAPL";
        trade.setStockTicker(ticker);
        assertEquals(ticker, trade.getStockTicker());
    }

    @Test
    @DisplayName("Should set and get quantity")
    void testSetAndGetQuantity() {
        Double quantity = 10.5;
        trade.setQuantity(quantity);
        assertEquals(quantity, trade.getQuantity());
    }

    @Test
    @DisplayName("Should set and get price")
    void testSetAndGetPrice() {
        BigDecimal price = BigDecimal.valueOf(185.92);
        trade.setPrice(price);
        assertEquals(price, trade.getPrice());
    }

    @Test
    @DisplayName("Should set and get trade type BUY")
    void testSetAndGetTradeTypeBuy() {
        trade.setTradeType(TradeType.BUY);
        assertEquals(TradeType.BUY, trade.getTradeType());
    }

    @Test
    @DisplayName("Should set and get trade type SELL")
    void testSetAndGetTradeTypeSell() {
        trade.setTradeType(TradeType.SELL);
        assertEquals(TradeType.SELL, trade.getTradeType());
    }

    @Test
    @DisplayName("Should set and get status PENDING")
    void testSetAndGetStatusPending() {
        trade.setStatus(TradeStatus.PENDING);
        assertEquals(TradeStatus.PENDING, trade.getStatus());
    }

    @Test
    @DisplayName("Should set and get status EXECUTED")
    void testSetAndGetStatusExecuted() {
        trade.setStatus(TradeStatus.EXECUTED);
        assertEquals(TradeStatus.EXECUTED, trade.getStatus());
    }

    @Test
    @DisplayName("Should set and get status CANCELLED")
    void testSetAndGetStatusCancelled() {
        trade.setStatus(TradeStatus.CANCELLED);
        assertEquals(TradeStatus.CANCELLED, trade.getStatus());
    }

    @Test
    @DisplayName("Should set and get status FAILED")
    void testSetAndGetStatusFailed() {
        trade.setStatus(TradeStatus.FAILED);
        assertEquals(TradeStatus.FAILED, trade.getStatus());
    }

    @Test
    @DisplayName("Should set and get execution date")
    void testSetAndGetExecutionDate() {
        LocalDateTime now = LocalDateTime.now();
        trade.setExecutionDate(now);
        assertEquals(now, trade.getExecutionDate());
    }

    @Test
    @DisplayName("Should calculate total value correctly")
    void testCalculateTotalValue() {
        trade.setQuantity(10.0);
        trade.setPrice(BigDecimal.valueOf(100.0));
        
        BigDecimal expectedTotal = BigDecimal.valueOf(1000.0);
        BigDecimal actualTotal = trade.getPrice().multiply(BigDecimal.valueOf(trade.getQuantity()));
        
        assertEquals(0, expectedTotal.compareTo(actualTotal));
    }

    @Test
    @DisplayName("Should handle zero quantity")
    void testZeroQuantity() {
        trade.setQuantity(0.0);
        assertEquals(0.0, trade.getQuantity());
    }

    @Test
    @DisplayName("Should handle fractional shares")
    void testFractionalShares() {
        trade.setQuantity(0.5);
        trade.setPrice(BigDecimal.valueOf(200.0));
        
        BigDecimal total = trade.getPrice().multiply(BigDecimal.valueOf(trade.getQuantity()));
        assertEquals(0, BigDecimal.valueOf(100.0).compareTo(total));
    }

    @Test
    @DisplayName("Should set and get position")
    void testSetAndGetPosition() {
        Position position = new Position();
        position.setId(1L);
        position.setStockTicker("AAPL");
        
        trade.setPosition(position);
        assertEquals(position, trade.getPosition());
    }

    @Test
    @DisplayName("Should set and get user")
    void testSetAndGetUser() {
        User user = new User();
        user.setId(1L);
        user.setUserName("testuser");
        
        trade.setUsers(user);
        assertEquals(user, trade.getUsers());
    }
}
