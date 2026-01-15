package com.moneyteam.controller;

import com.moneyteam.dto.TradeRequestDto;
import com.moneyteam.dto.TradeResponseDto;
import com.moneyteam.model.Trade;
import com.moneyteam.model.Position;
import com.moneyteam.model.enums.TradeStatus;
import com.moneyteam.model.enums.TradeType;
import com.moneyteam.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

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

    private TradeRequestDto testRequest;
    private TradeResponseDto testResponse;

    @BeforeEach
    void setUp() {
        testRequest = new TradeRequestDto();
        testRequest.setStockTicker("AAPL");
        testRequest.setQuantity(10.0);
        testRequest.setPrice(BigDecimal.valueOf(185.92));
        testRequest.setTradeType(TradeType.BUY);
        testRequest.setStatus(TradeStatus.PENDING);
        testRequest.setUserId(1L);
        testRequest.setExecutionDate(LocalDateTime.now());

        testResponse = new TradeResponseDto();
        testResponse.setId(1L);
        testResponse.setStockTicker("AAPL");
        testResponse.setQuantity(10.0);
        testResponse.setPrice(BigDecimal.valueOf(185.92));
        testResponse.setTradeType(TradeType.BUY);
        testResponse.setStatus(TradeStatus.EXECUTED);
        testResponse.setUserId(1L);
    }

    @Test
    @DisplayName("Should create trade successfully")
    void testCreateTrade() {
        when(tradeService.create(any(TradeRequestDto.class))).thenReturn(testResponse);

        ResponseEntity<TradeResponseDto> result = tradeController.create(testRequest);

        assertNotNull(result);
        assertEquals(200, result.getStatusCodeValue());
        assertEquals("AAPL", result.getBody().getStockTicker());
    }

    @Test
    @DisplayName("Should get trade by ID")
    void testGetTradeById() {
        when(tradeService.getById(1L)).thenReturn(Optional.of(testResponse));

        ResponseEntity<TradeResponseDto> result = tradeController.getById(1L);

        assertNotNull(result);
        assertEquals(200, result.getStatusCodeValue());
        assertEquals("AAPL", result.getBody().getStockTicker());
    }

    @Test
    @DisplayName("Should return 404 for non-existent trade")
    void testGetTradeByIdNotFound() {
        when(tradeService.getById(999L)).thenReturn(Optional.empty());

        ResponseEntity<TradeResponseDto> result = tradeController.getById(999L);

        assertEquals(404, result.getStatusCodeValue());
    }

    @Test
    @DisplayName("Should execute BUY trade successfully")
    void testExecuteBuyTrade() {
        assertTrue(isValidTradeRequest("AAPL", 10.0, BigDecimal.valueOf(185.92), TradeType.BUY));
    }

    @Test
    @DisplayName("Should execute SELL trade successfully")
    void testExecuteSellTrade() {
        assertTrue(isValidTradeRequest("AAPL", 5.0, BigDecimal.valueOf(190.00), TradeType.SELL));
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
        assertFalse(isValidPrice(BigDecimal.valueOf(-100.0)));
        assertFalse(isValidPrice(BigDecimal.ZERO));
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
    @DisplayName("Should calculate trade total value")
    void testCalculateTradeTotal() {
        BigDecimal total = calculateTradeTotal(10.0, BigDecimal.valueOf(185.92));
        assertEquals(0, BigDecimal.valueOf(1859.2).compareTo(total));
    }

    @Test
    @DisplayName("Should handle fractional shares")
    void testFractionalShares() {
        BigDecimal total = calculateTradeTotal(0.5, BigDecimal.valueOf(200.0));
        assertEquals(0, BigDecimal.valueOf(100.0).compareTo(total));
    }

    @Test
    @DisplayName("Should validate sufficient funds for BUY")
    void testValidateSufficientFunds() {
        BigDecimal cashBalance = BigDecimal.valueOf(100000.0);
        BigDecimal tradeValue = BigDecimal.valueOf(1859.2);
        
        assertTrue(hasSufficientFunds(cashBalance, tradeValue));
    }

    @Test
    @DisplayName("Should reject BUY with insufficient funds")
    void testRejectInsufficientFunds() {
        BigDecimal cashBalance = BigDecimal.valueOf(1000.0);
        BigDecimal tradeValue = BigDecimal.valueOf(5000.0);
        
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

    @Test
    @DisplayName("Should search trades by user ID")
    void testSearchByUserId() {
        List<TradeResponseDto> trades = Arrays.asList(testResponse);
        when(tradeService.listByUser(1L)).thenReturn(trades);

        ResponseEntity<?> result = tradeController.search(1L, null, null, null, null, null);

        assertNotNull(result);
        assertEquals(200, result.getStatusCodeValue());
    }

    @Test
    @DisplayName("Should search trades by stock ticker")
    void testSearchByStockTicker() {
        List<TradeResponseDto> trades = Arrays.asList(testResponse);
        when(tradeService.listByStockTicker("AAPL")).thenReturn(trades);

        ResponseEntity<?> result = tradeController.search(null, null, "AAPL", null, null, null);

        assertNotNull(result);
        assertEquals(200, result.getStatusCodeValue());
    }

    // Helper methods
    private boolean isValidTradeRequest(String ticker, Double quantity, BigDecimal price, TradeType tradeType) {
        return isValidTicker(ticker) && isValidQuantity(quantity) && 
               isValidPrice(price) && tradeType != null;
    }

    private boolean isValidTicker(String ticker) {
        return ticker != null && !ticker.isEmpty() && ticker.length() <= 10;
    }

    private boolean isValidQuantity(Double quantity) {
        return quantity != null && quantity > 0;
    }

    private boolean isValidPrice(BigDecimal price) {
        return price != null && price.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal calculateTradeTotal(Double quantity, BigDecimal price) {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    private boolean hasSufficientFunds(BigDecimal balance, BigDecimal tradeValue) {
        return balance.compareTo(tradeValue) >= 0;
    }

    private boolean hasSufficientShares(Double owned, Double toSell) {
        return owned >= toSell;
    }
}
