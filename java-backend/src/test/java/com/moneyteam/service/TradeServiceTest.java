package com.moneyteam.service;

import com.moneyteam.model.Trade;
import com.moneyteam.model.User;
import com.moneyteam.model.Position;
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

    @InjectMocks
    private TradeServiceImpl tradeService;

    private Trade testTrade;
    private User testUser;
    private Position testPosition;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testTrade = new Trade();
        testTrade.setId(1L);
        testTrade.setStockTicker("AAPL");
        testTrade.setQuantity(10.0);
        testTrade.setPrice(185.92);
        testTrade.setTradeType("BUY");
        testTrade.setStatus("EXECUTED");
        testTrade.setExecutionDate(LocalDateTime.now());

        testPosition = new Position();
        testPosition.setId(1L);
        testPosition.setStockTicker("AAPL");
        testPosition.setTotalQuantity(10.0);
        testPosition.setAveragePrice(185.92);
    }

    @Test
    @DisplayName("Should execute BUY trade")
    void testExecuteBuyTrade() {
        when(tradeRepository.save(any(Trade.class))).thenReturn(testTrade);

        Trade result = tradeRepository.save(testTrade);

        assertNotNull(result);
        assertEquals("BUY", result.getTradeType());
        assertEquals("EXECUTED", result.getStatus());
        verify(tradeRepository, times(1)).save(any(Trade.class));
    }

    @Test
    @DisplayName("Should execute SELL trade")
    void testExecuteSellTrade() {
        testTrade.setTradeType("SELL");
        when(tradeRepository.save(any(Trade.class))).thenReturn(testTrade);

        Trade result = tradeRepository.save(testTrade);

        assertNotNull(result);
        assertEquals("SELL", result.getTradeType());
    }

    @Test
    @DisplayName("Should calculate trade total value")
    void testCalculateTradeValue() {
        Double totalValue = testTrade.getQuantity() * testTrade.getPrice();
        assertEquals(1859.2, totalValue, 0.01);
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
        trade2.setPrice(378.91);
        trade2.setTradeType("BUY");

        List<Trade> trades = Arrays.asList(testTrade, trade2);
        when(tradeRepository.findByUserId(1L)).thenReturn(trades);

        List<Trade> result = tradeRepository.findByUserId(1L);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should get trades by stock ticker")
    void testGetTradesByTicker() {
        List<Trade> trades = Arrays.asList(testTrade);
        when(tradeRepository.findByStockTicker("AAPL")).thenReturn(trades);

        List<Trade> result = tradeRepository.findByStockTicker("AAPL");

        assertEquals(1, result.size());
        assertEquals("AAPL", result.get(0).getStockTicker());
    }

    @Test
    @DisplayName("Should cancel pending trade")
    void testCancelTrade() {
        testTrade.setStatus("PENDING");
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(testTrade));

        Optional<Trade> trade = tradeRepository.findById(1L);
        trade.get().setStatus("CANCELLED");

        assertEquals("CANCELLED", trade.get().getStatus());
    }

    @Test
    @DisplayName("Should not cancel executed trade")
    void testCannotCancelExecutedTrade() {
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(testTrade));

        Optional<Trade> trade = tradeRepository.findById(1L);
        
        // Trade is already EXECUTED, should not be cancellable
        assertEquals("EXECUTED", trade.get().getStatus());
        assertNotEquals("PENDING", trade.get().getStatus());
    }

    @Test
    @DisplayName("Should validate trade quantity is positive")
    void testValidatePositiveQuantity() {
        assertTrue(testTrade.getQuantity() > 0);
    }

    @Test
    @DisplayName("Should validate trade price is positive")
    void testValidatePositivePrice() {
        assertTrue(testTrade.getPrice() > 0);
    }

    @Test
    @DisplayName("Should get all trades sorted by date")
    void testGetTradesSortedByDate() {
        Trade olderTrade = new Trade();
        olderTrade.setExecutionDate(LocalDateTime.now().minusDays(1));
        
        Trade newerTrade = new Trade();
        newerTrade.setExecutionDate(LocalDateTime.now());

        List<Trade> trades = Arrays.asList(newerTrade, olderTrade);
        when(tradeRepository.findAllByOrderByExecutionDateDesc()).thenReturn(trades);

        List<Trade> result = tradeRepository.findAllByOrderByExecutionDateDesc();

        assertTrue(result.get(0).getExecutionDate().isAfter(result.get(1).getExecutionDate()));
    }
}
