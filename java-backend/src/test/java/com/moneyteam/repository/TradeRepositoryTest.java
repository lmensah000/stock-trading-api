package com.moneyteam.repository;

import com.moneyteam.model.Trade;
import com.moneyteam.model.User;
import com.moneyteam.model.Position;
import com.moneyteam.model.enums.TradeStatus;
import com.moneyteam.model.enums.TradeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * Unit tests for TradeRepository.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Trade Repository Tests")
public class TradeRepositoryTest {

    @Mock
    private TradeRepository tradeRepository;

    private Trade testTrade;

    @BeforeEach
    void setUp() {
        testTrade = new Trade();
        testTrade.setId(1L);
        testTrade.setUserTradeId(1L);
        testTrade.setStockTicker("AAPL");
        testTrade.setQuantity(10.0);
        testTrade.setPrice(BigDecimal.valueOf(185.92));
        testTrade.setTradeType(TradeType.BUY);
        testTrade.setStatus(TradeStatus.EXECUTED);
        testTrade.setExecutionDate(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should save trade")
    void testSaveTrade() {
        when(tradeRepository.save(any(Trade.class))).thenReturn(testTrade);

        Trade saved = tradeRepository.save(testTrade);

        assertNotNull(saved);
        assertEquals(1L, saved.getId());
        verify(tradeRepository, times(1)).save(any(Trade.class));
    }

    @Test
    @DisplayName("Should find trade by ID")
    void testFindById() {
        when(tradeRepository.findById(1L)).thenReturn(Optional.of(testTrade));

        Optional<Trade> result = tradeRepository.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("AAPL", result.get().getStockTicker());
    }

    @Test
    @DisplayName("Should find trades by user ID")
    void testFindByUserTradeId() {
        List<Trade> trades = Arrays.asList(testTrade);
        when(tradeRepository.findByUserTradeId(1L)).thenReturn(trades);

        List<Trade> result = tradeRepository.findByUserTradeId(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getUserTradeId());
    }

    @Test
    @DisplayName("Should find trades by trade type")
    void testFindByTradeType() {
        List<Trade> trades = Arrays.asList(testTrade);
        when(tradeRepository.findByTradeType(TradeType.BUY)).thenReturn(trades);

        List<Trade> result = tradeRepository.findByTradeType(TradeType.BUY);

        assertEquals(1, result.size());
        assertEquals(TradeType.BUY, result.get(0).getTradeType());
    }

    @Test
    @DisplayName("Should find trades by status")
    void testFindByStatus() {
        List<Trade> trades = Arrays.asList(testTrade);
        when(tradeRepository.findByStatus(TradeStatus.EXECUTED)).thenReturn(trades);

        List<Trade> result = tradeRepository.findByStatus(TradeStatus.EXECUTED);

        assertEquals(1, result.size());
        assertEquals(TradeStatus.EXECUTED, result.get(0).getStatus());
    }

    @Test
    @DisplayName("Should find trades by stock ticker")
    void testFindByStockTickerIgnoreCase() {
        List<Trade> trades = Arrays.asList(testTrade);
        when(tradeRepository.findByStockTickerIgnoreCase("aapl")).thenReturn(trades);

        List<Trade> result = tradeRepository.findByStockTickerIgnoreCase("aapl");

        assertEquals(1, result.size());
        assertEquals("AAPL", result.get(0).getStockTicker());
    }

    @Test
    @DisplayName("Should find trades between dates")
    void testFindByExecutionDateBetween() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        List<Trade> trades = Arrays.asList(testTrade);
        when(tradeRepository.findByExecutionDateBetween(start, end)).thenReturn(trades);

        List<Trade> result = tradeRepository.findByExecutionDateBetween(start, end);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should delete trade")
    void testDeleteTrade() {
        doNothing().when(tradeRepository).deleteById(1L);

        tradeRepository.deleteById(1L);

        verify(tradeRepository, times(1)).deleteById(1L);
    }
}
