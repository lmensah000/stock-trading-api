package com.moneyteam.trading.repository;

import com.moneyteam.trading.model.Trade;
import com.moneyteam.trading.model.enums.TradeStatus;
import com.moneyteam.trading.model.enums.OrderSide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByUserTradeId(@Param("userId") Long userId);

    Optional<Trade> findByIdAndUserTradeId(Long id, Long userId);

    // Every filter below is scoped by user: an unscoped variant would return
    // other users' trade history to whoever called the endpoint.
    List<Trade> findByUserTradeIdAndSide(Long userId, OrderSide side);

    List<Trade> findByUserTradeIdAndStatus(Long userId, TradeStatus status);

    List<Trade> findByUserTradeIdAndStockTickerIgnoreCase(Long userId, String stockTicker);

    List<Trade> findByUserTradeIdAndExecutionDateBetween(Long userId, LocalDateTime start, LocalDateTime end);
}