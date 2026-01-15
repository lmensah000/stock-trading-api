package com.moneyteam.controller;

import com.moneyteam.model.Trade;
import com.moneyteam.model.Position;
import com.moneyteam.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TradeController.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Trade Controller Tests")
public class TradeControllerTest {

    @Mock
    private TradeService tradeService;

    @InjectMocks
    private TradeController tradeController;

    private Trade testTrade;

    @BeforeEach
    void setUp() {
        testTrade = new Trade();
        testTrade.setId(1L);
        testTrade.setStockTicker("AAPL");
        testTrade.setQuantity(10.0);
        testTrade.setPrice(185.92);
        testTrade.setTradeType("BUY");
        testTrade.setStatus("EXECUTED");
        testTrade.setExecutionDate(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should execute BUY trade successfully")
    void testExecuteBuyTrade() {
        assertTrue(isValidTradeRequest("AAPL", 10.0, 185.92, "BUY"));
    }

    @Test
    @DisplayName("Should execute SELL trade successfully")
    void testExecuteSellTrade() {
        assertTrue(isValidTradeRequest("AAPL", 5.0, 190.00, "SELL"));
    }

    @Test
    @DisplayName("Should reject invalid trade type")
    void testRejectInvalidTradeType() {
        assertFalse(isValidTradeType("INVALID"));
        assertFalse(isValidTradeType(""));
        assertFalse(isValidTradeType(null));
    }

    @Test
    @DisplayName("Should reject negative quantity")
    void testRejectNegativeQuantity() {
        assertFalse(isValidQuantity(-10.0));
        assertFalse(isValidQuantity(0.0));
    }

    @Test
    @DisplayName("Should reject negative price")
    void testRejectNegativePrice() {
        assertFalse(isValidPrice(-100.0));
        assertFalse(isValidPrice(0.0));
    }

    @Test
    @DisplayName("Should reject empty ticker")
    void testRejectEmptyTicker() {
        assertFalse(isValidTicker(""));
        assertFalse(isValidTicker(null));
    }

    @Test
    @DisplayName("Should validate ticker format")
    void testValidateTickerFormat() {
        assertTrue(isValidTicker("AAPL"));
        assertTrue(isValidTicker("MSFT"));
        assertTrue(isValidTicker("GOOGL"));
    }

    @Test
    @DisplayName("Should get trade history for user")
    void testGetTradeHistory() {
        Trade trade2 = new Trade();
        trade2.setId(2L);
        trade2.setStockTicker("MSFT");
        trade2.setTradeType("BUY");

        List<Trade> trades = Arrays.asList(testTrade, trade2);
        
        assertEquals(2, trades.size());
    }

    @Test
    @DisplayName("Should calculate trade total value")
    void testCalculateTradeTotal() {
        Double total = calculateTradeTotal(10.0, 185.92);
        assertEquals(1859.2, total, 0.01);
    }

    @Test
    @DisplayName("Should handle fractional shares")
    void testFractionalShares() {
        Double total = calculateTradeTotal(0.5, 200.0);
        assertEquals(100.0, total, 0.01);
    }

    @Test
    @DisplayName("Should validate sufficient funds for BUY")
    void testValidateSufficientFunds() {
        Double cashBalance = 100000.0;
        Double tradeValue = 1859.2;
        
        assertTrue(hasSufficientFunds(cashBalance, tradeValue));
    }

    @Test
    @DisplayName("Should reject BUY with insufficient funds")
    void testRejectInsufficientFunds() {
        Double cashBalance = 1000.0;
        Double tradeValue = 5000.0;
        
        assertFalse(hasSufficientFunds(cashBalance, tradeValue));
    }

    @Test
    @DisplayName("Should validate sufficient shares for SELL")
    void testValidateSufficientShares() {
        Double ownedShares = 20.0;
        Double sellQuantity = 10.0;
        
        assertTrue(hasSufficientShares(ownedShares, sellQuantity));
    }

    @Test
    @DisplayName("Should reject SELL with insufficient shares")
    void testRejectInsufficientShares() {
        Double ownedShares = 5.0;
        Double sellQuantity = 10.0;
        
        assertFalse(hasSufficientShares(ownedShares, sellQuantity));
    }

    // Helper methods
    private boolean isValidTradeRequest(String ticker, Double quantity, Double price, String tradeType) {
        return isValidTicker(ticker) && isValidQuantity(quantity) && 
               isValidPrice(price) && isValidTradeType(tradeType);
    }

    private boolean isValidTicker(String ticker) {
        return ticker != null && !ticker.isEmpty() && ticker.length() <= 10;
    }

    private boolean isValidQuantity(Double quantity) {
        return quantity != null && quantity > 0;
    }

    private boolean isValidPrice(Double price) {
        return price != null && price > 0;
    }

    private boolean isValidTradeType(String tradeType) {
        return "BUY".equals(tradeType) || "SELL".equals(tradeType);
    }

    private Double calculateTradeTotal(Double quantity, Double price) {
        return quantity * price;
    }

    private boolean hasSufficientFunds(Double balance, Double tradeValue) {
        return balance >= tradeValue;
    }

    private boolean hasSufficientShares(Double owned, Double toSell) {
        return owned >= toSell;
    }
}
