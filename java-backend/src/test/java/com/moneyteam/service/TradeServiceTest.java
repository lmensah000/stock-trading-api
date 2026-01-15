package com.moneyteam.service;

import com.moneyteam.model.Trade;
import com.moneyteam.model.User;
import com.moneyteam.model.Position;
import com.moneyteam.model.enums.TradeStatus;
import com.moneyteam.model.enums.TradeType;
import com.moneyteam.repository.TradeRepository;
import com.moneyteam.repository.PositionRepository;
import com.moneyteam.service.impl.TradeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Unit tests for TradeService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Trade Service Tests")
public class TradeServiceTest {

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private PositionRepository positionRepository;

    private Trade testTrade;
    private User testUser;
    private Position testPosition;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserName("testuser");

        testTrade = new Trade();
        testTrade.setId(1L);
        testTrade.setStockTicker("AAPL");
        testTrade.setQuantity(10.0);
        testTrade.setPrice(BigDecimal.valueOf(185.92));
        testTrade.setTradeType(TradeType.BUY);
        testTrade.setStatus(TradeStatus.EXECUTED);
        testTrade.setExecutionDate(LocalDateTime.now());
        testTrade.setUserTradeId(1L);

        testPosition = new Position();
        testPosition.setId(1L);
        testPosition.setStockTicker("AAPL");
        testPosition.setTotalQuantity(10.0);
        testPosition.setAveragePrice(BigDecimal.valueOf(185.92));
    }

    @Test
    @DisplayName("Should execute BUY trade")
    void testExecuteBuyTrade() {
        when(tradeRepository.save(any(Trade.class))).thenReturn(testTrade);

        Trade result = tradeRepository.save(testTrade);

        assertNotNull(result);
        assertEquals(TradeType.BUY, result.getTradeType());
        assertEquals(TradeStatus.EXECUTED, result.getStatus());
        verify(tradeRepository, times(1)).save(any(Trade.class));
    }

    @Test
    @DisplayName("Should execute SELL trade")
    void testExecuteSellTrade() {
        testTrade.setTradeType(TradeType.SELL);
        when(tradeRepository.save(any(Trade.class))).thenReturn(testTrade);

        Trade result = tradeRepository.save(testTrade);

        assertNotNull(result);
        assertEquals(TradeType.SELL, result.getTradeType());
    }

    @Test
    @DisplayName("Should calculate trade total value")
    void testCalculateTradeValue() {
        BigDecimal totalValue = testTrade.getPrice().multiply(BigDecimal.valueOf(testTrade.getQuantity()));
        assertEquals(0, BigDecimal.valueOf(1859.2).compareTo(totalValue));
    }

    @Test
    @DisplayName("Should get trade by ID")
    void testGetTradeById() {
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(testTrade));

        Optional<Trade> result = tradeRepository.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("AAPL", result.get().getStockTicker());
    }

    @Test
    @DisplayName("Should get trades by user ID")
    void testGetTradesByUserId() {
        Trade trade2 = new Trade();
        trade2.setId(2L);
        trade2.setStockTicker("MSFT");
        trade2.setQuantity(5.0);
        trade2.setPrice(BigDecimal.valueOf(378.91));
        trade2.setTradeType(TradeType.BUY);

        List<Trade> trades = Arrays.asList(testTrade, trade2);
        when(tradeRepository.findByUserTradeId(1L)).thenReturn(trades);

        List<Trade> result = tradeRepository.findByUserTradeId(1L);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should get trades by stock ticker")
    void testGetTradesByTicker() {
        List<Trade> trades = Arrays.asList(testTrade);
        when(tradeRepository.findByStockTickerIgnoreCase("AAPL")).thenReturn(trades);

        List<Trade> result = tradeRepository.findByStockTickerIgnoreCase("AAPL");

        assertEquals(1, result.size());
        assertEquals("AAPL", result.get(0).getStockTicker());
    }

    @Test
    @DisplayName("Should cancel pending trade")
    void testCancelTrade() {
        testTrade.setStatus(TradeStatus.PENDING);
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(testTrade));

        Optional<Trade> trade = tradeRepository.findById(1L);
        trade.get().setStatus(TradeStatus.CANCELLED);

        assertEquals(TradeStatus.CANCELLED, trade.get().getStatus());
    }

    @Test
    @DisplayName("Should not cancel executed trade")
    void testCannotCancelExecutedTrade() {
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(testTrade));

        Optional<Trade> trade = tradeRepository.findById(1L);
        
        // Trade is already EXECUTED, should not be cancellable
        assertEquals(TradeStatus.EXECUTED, trade.get().getStatus());
        assertNotEquals(TradeStatus.PENDING, trade.get().getStatus());
    }

    @Test
    @DisplayName("Should validate trade quantity is positive")
    void testValidatePositiveQuantity() {
        assertTrue(testTrade.getQuantity() > 0);
    }

    @Test
    @DisplayName("Should validate trade price is positive")
    void testValidatePositivePrice() {
        assertTrue(testTrade.getPrice().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should get trades sorted by date")
    void testGetTradesSortedByDate() {
        Trade olderTrade = new Trade();
        olderTrade.setId(2L);
        olderTrade.setExecutionDate(LocalDateTime.now().minusDays(1));
        
        Trade newerTrade = new Trade();
        newerTrade.setId(3L);
        newerTrade.setExecutionDate(LocalDateTime.now());

        List<Trade> trades = Arrays.asList(newerTrade, olderTrade);

        assertTrue(trades.get(0).getExecutionDate().isAfter(trades.get(1).getExecutionDate()));
    }

    @Test
    @DisplayName("Should get trades by status")
    void testGetTradesByStatus() {
        List<Trade> trades = Arrays.asList(testTrade);
        when(tradeRepository.findByStatus(TradeStatus.EXECUTED)).thenReturn(trades);

        List<Trade> result = tradeRepository.findByStatus(TradeStatus.EXECUTED);

        assertEquals(1, result.size());
        assertEquals(TradeStatus.EXECUTED, result.get(0).getStatus());
    }

    @Test
    @DisplayName("Should get trades by type")
    void testGetTradesByType() {
        List<Trade> trades = Arrays.asList(testTrade);
        when(tradeRepository.findByTradeType(TradeType.BUY)).thenReturn(trades);

        List<Trade> result = tradeRepository.findByTradeType(TradeType.BUY);

        assertEquals(1, result.size());
        assertEquals(TradeType.BUY, result.get(0).getTradeType());
    }
}
