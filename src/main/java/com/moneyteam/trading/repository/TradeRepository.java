package com.moneyteam.trading.repository;

import com.moneyteam.trading.model.Trade;
import com.moneyteam.trading.model.enums.TradeStatus;
import com.moneyteam.trading.model.enums.OrderSide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByUserTradeId(@Param("userId") Long userId);

    List<Trade> findBySide(OrderSide side);

    List<Trade> findByStatus(TradeStatus status);

    List<Trade> findByStockTickerIgnoreCase(String stockTicker);

    List<Trade> findByExecutionDateBetween(LocalDateTime start, LocalDateTime end);
}