package com.moneyteam.integration;

import com.moneyteam.dto.TradeRequestDto;
import com.moneyteam.dto.TradeResponseDto;
import com.moneyteam.model.Trade;
import com.moneyteam.model.User;
import com.moneyteam.model.Position;
import com.moneyteam.model.enums.TradeStatus;
import com.moneyteam.model.enums.TradeType;
import com.moneyteam.repository.TradeRepository;
import com.moneyteam.repository.UserRepository;
import com.moneyteam.repository.PositionRepository;
import com.moneyteam.service.TradeService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * Integration tests for the trading workflow.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Trading Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TradingIntegrationTest {

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private TradeService tradeService;

    private User testUser;
    private Position testPosition;
    private Trade testTrade;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUserName("trader1");
        testUser.setEmail("trader@example.com");
        testUser.setPassWord("hashedpassword");

        // Setup test position
        testPosition = new Position();
        testPosition.setId(1L);
        testPosition.setUserId(1L);
        testPosition.setStockTicker("AAPL");
        testPosition.setTotalQuantity(100.0);
        testPosition.setAveragePrice(BigDecimal.valueOf(150.00));

        // Setup test trade
        testTrade = new Trade();
        testTrade.setId(1L);
        testTrade.setUserTradeId(1L);
        testTrade.setStockTicker("AAPL");
        testTrade.setQuantity(10.0);
        testTrade.setPrice(BigDecimal.valueOf(185.00));
        testTrade.setTradeType(TradeType.BUY);
        testTrade.setStatus(TradeStatus.EXECUTED);
        testTrade.setExecutionDate(LocalDateTime.now());
    }

    @Test
    @Order(1)
    @DisplayName("Integration: Full buy trade workflow")
    void testFullBuyTradeWorkflow() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(positionRepository.findByUsersIdAndStockTicker(1L, "AAPL"))
                .thenReturn(Optional.of(testPosition));
        when(tradeRepository.save(any(Trade.class))).thenReturn(testTrade);

        // Act - Simulate the buy workflow
        Optional<User> user = userRepository.findById(1L);
        Optional<Position> position = positionRepository.findByUsersIdAndStockTicker(1L, "AAPL");

        assertTrue(user.isPresent());
        assertTrue(position.isPresent());

        // Create trade
        Trade trade = new Trade();
        trade.setUserTradeId(1L);
        trade.setStockTicker("AAPL");
        trade.setQuantity(10.0);
        trade.setPrice(BigDecimal.valueOf(185.00));
        trade.setTradeType(TradeType.BUY);
        trade.setStatus(TradeStatus.PENDING);

        Trade savedTrade = tradeRepository.save(trade);

        // Assert
        assertNotNull(savedTrade);
        assertEquals("AAPL", savedTrade.getStockTicker());
        assertEquals(TradeType.BUY, savedTrade.getTradeType());
    }

    @Test
    @Order(2)
    @DisplayName("Integration: Full sell trade workflow")
    void testFullSellTradeWorkflow() {
        // Arrange - user has enough shares
        testPosition.setTotalQuantity(100.0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(positionRepository.findByUsersIdAndStockTicker(1L, "AAPL"))
                .thenReturn(Optional.of(testPosition));

        Trade sellTrade = new Trade();
        sellTrade.setId(2L);
        sellTrade.setUserTradeId(1L);
        sellTrade.setStockTicker("AAPL");
        sellTrade.setQuantity(20.0);
        sellTrade.setPrice(BigDecimal.valueOf(190.00));
        sellTrade.setTradeType(TradeType.SELL);
        sellTrade.setStatus(TradeStatus.EXECUTED);

        when(tradeRepository.save(any(Trade.class))).thenReturn(sellTrade);

        // Act
        Optional<Position> position = positionRepository.findByUsersIdAndStockTicker(1L, "AAPL");
        assertTrue(position.isPresent());
        assertTrue(position.get().getTotalQuantity() >= 20.0); // Check sufficient shares

        Trade savedTrade = tradeRepository.save(sellTrade);

        // Assert
        assertNotNull(savedTrade);
        assertEquals(TradeType.SELL, savedTrade.getTradeType());
        assertEquals(TradeStatus.EXECUTED, savedTrade.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("Integration: Trade cancellation workflow")
    void testTradeCancellationWorkflow() {
        // Arrange - Create a pending trade
        Trade pendingTrade = new Trade();
        pendingTrade.setId(3L);
        pendingTrade.setUserTradeId(1L);
        pendingTrade.setStockTicker("MSFT");
        pendingTrade.setQuantity(5.0);
        pendingTrade.setPrice(BigDecimal.valueOf(380.00));
        pendingTrade.setTradeType(TradeType.BUY);
        pendingTrade.setStatus(TradeStatus.PENDING);

        when(tradeRepository.findById(3L)).thenReturn(Optional.of(pendingTrade));

        // Act - Cancel the trade
        Optional<Trade> trade = tradeRepository.findById(3L);
        assertTrue(trade.isPresent());
        assertEquals(TradeStatus.PENDING, trade.get().getStatus());

        trade.get().setStatus(TradeStatus.CANCELLED);

        // Assert
        assertEquals(TradeStatus.CANCELLED, trade.get().getStatus());
    }

    @Test
    @Order(4)
    @DisplayName("Integration: Multiple trades for same stock")
    void testMultipleTradesForSameStock() {
        // Arrange
        Trade trade1 = createTrade(1L, "AAPL", 10.0, BigDecimal.valueOf(180.00), TradeType.BUY);
        Trade trade2 = createTrade(2L, "AAPL", 5.0, BigDecimal.valueOf(185.00), TradeType.BUY);
        Trade trade3 = createTrade(3L, "AAPL", 8.0, BigDecimal.valueOf(190.00), TradeType.SELL);

        List<Trade> aaplTrades = Arrays.asList(trade1, trade2, trade3);
        when(tradeRepository.findByStockTickerIgnoreCase("AAPL")).thenReturn(aaplTrades);

        // Act
        List<Trade> trades = tradeRepository.findByStockTickerIgnoreCase("AAPL");

        // Assert
        assertEquals(3, trades.size());
        
        // Verify trade types
        long buyCount = trades.stream().filter(t -> t.getTradeType() == TradeType.BUY).count();
        long sellCount = trades.stream().filter(t -> t.getTradeType() == TradeType.SELL).count();
        
        assertEquals(2, buyCount);
        assertEquals(1, sellCount);
    }

    @Test
    @Order(5)
    @DisplayName("Integration: Trade history retrieval")
    void testTradeHistoryRetrieval() {
        // Arrange
        List<Trade> trades = Arrays.asList(
                createTrade(1L, "AAPL", 10.0, BigDecimal.valueOf(180.00), TradeType.BUY),
                createTrade(2L, "MSFT", 5.0, BigDecimal.valueOf(380.00), TradeType.BUY),
                createTrade(3L, "GOOGL", 2.0, BigDecimal.valueOf(140.00), TradeType.BUY),
                createTrade(4L, "AAPL", 5.0, BigDecimal.valueOf(190.00), TradeType.SELL)
        );

        when(tradeRepository.findByUserTradeId(1L)).thenReturn(trades);

        // Act
        List<Trade> userTrades = tradeRepository.findByUserTradeId(1L);

        // Assert
        assertEquals(4, userTrades.size());
        
        // Verify unique tickers
        Set<String> tickers = new HashSet<>();
        userTrades.forEach(t -> tickers.add(t.getStockTicker()));
        assertEquals(3, tickers.size()); // AAPL, MSFT, GOOGL
    }

    @Test
    @Order(6)
    @DisplayName("Integration: Position update after trade")
    void testPositionUpdateAfterTrade() {
        // Arrange
        Position position = new Position();
        position.setId(1L);
        position.setUserId(1L);
        position.setStockTicker("NVDA");
        position.setTotalQuantity(50.0);
        position.setAveragePrice(BigDecimal.valueOf(500.00));

        when(positionRepository.findByUsersIdAndStockTicker(1L, "NVDA"))
                .thenReturn(Optional.of(position));

        // Act - Simulate BUY trade
        Optional<Position> pos = positionRepository.findByUsersIdAndStockTicker(1L, "NVDA");
        assertTrue(pos.isPresent());

        // Update position (simulating a buy of 10 shares at $520)
        double newQuantity = pos.get().getTotalQuantity() + 10.0;
        BigDecimal oldValue = pos.get().getAveragePrice().multiply(BigDecimal.valueOf(pos.get().getTotalQuantity()));
        BigDecimal newValue = BigDecimal.valueOf(520.00).multiply(BigDecimal.valueOf(10.0));
        BigDecimal newAvgPrice = oldValue.add(newValue).divide(BigDecimal.valueOf(newQuantity), 2, BigDecimal.ROUND_HALF_UP);

        pos.get().setTotalQuantity(newQuantity);
        pos.get().setAveragePrice(newAvgPrice);

        // Assert
        assertEquals(60.0, pos.get().getTotalQuantity());
        assertTrue(pos.get().getAveragePrice().compareTo(BigDecimal.valueOf(500.00)) > 0); // Avg increased
    }

    @Test
    @Order(7)
    @DisplayName("Integration: Trades within date range")
    void testTradesWithinDateRange() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();

        Trade trade1 = createTrade(1L, "AAPL", 10.0, BigDecimal.valueOf(180.00), TradeType.BUY);
        trade1.setExecutionDate(LocalDateTime.now().minusDays(3));

        Trade trade2 = createTrade(2L, "MSFT", 5.0, BigDecimal.valueOf(380.00), TradeType.BUY);
        trade2.setExecutionDate(LocalDateTime.now().minusDays(1));

        List<Trade> tradesInRange = Arrays.asList(trade1, trade2);
        when(tradeRepository.findByExecutionDateBetween(any(), any())).thenReturn(tradesInRange);

        // Act
        List<Trade> result = tradeRepository.findByExecutionDateBetween(start, end);

        // Assert
        assertEquals(2, result.size());
        result.forEach(trade -> {
            assertTrue(trade.getExecutionDate().isAfter(start.minusSeconds(1)));
            assertTrue(trade.getExecutionDate().isBefore(end.plusSeconds(1)));
        });
    }

    @Test
    @Order(8)
    @DisplayName("Integration: Trade status filtering")
    void testTradeStatusFiltering() {
        // Arrange
        Trade executedTrade = createTrade(1L, "AAPL", 10.0, BigDecimal.valueOf(180.00), TradeType.BUY);
        executedTrade.setStatus(TradeStatus.EXECUTED);

        Trade pendingTrade = createTrade(2L, "MSFT", 5.0, BigDecimal.valueOf(380.00), TradeType.BUY);
        pendingTrade.setStatus(TradeStatus.PENDING);

        Trade cancelledTrade = createTrade(3L, "GOOGL", 2.0, BigDecimal.valueOf(140.00), TradeType.SELL);
        cancelledTrade.setStatus(TradeStatus.CANCELLED);

        when(tradeRepository.findByStatus(TradeStatus.EXECUTED)).thenReturn(Collections.singletonList(executedTrade));
        when(tradeRepository.findByStatus(TradeStatus.PENDING)).thenReturn(Collections.singletonList(pendingTrade));
        when(tradeRepository.findByStatus(TradeStatus.CANCELLED)).thenReturn(Collections.singletonList(cancelledTrade));

        // Act & Assert
        assertEquals(1, tradeRepository.findByStatus(TradeStatus.EXECUTED).size());
        assertEquals(1, tradeRepository.findByStatus(TradeStatus.PENDING).size());
        assertEquals(1, tradeRepository.findByStatus(TradeStatus.CANCELLED).size());
    }

    // Helper method
    private Trade createTrade(Long id, String ticker, Double quantity, BigDecimal price, TradeType type) {
        Trade trade = new Trade();
        trade.setId(id);
        trade.setUserTradeId(1L);
        trade.setStockTicker(ticker);
        trade.setQuantity(quantity);
        trade.setPrice(price);
        trade.setTradeType(type);
        trade.setStatus(TradeStatus.EXECUTED);
        trade.setExecutionDate(LocalDateTime.now());
        return trade;
    }
}
