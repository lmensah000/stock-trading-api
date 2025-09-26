package com.moneyteam.repository;

import com.moneyteam.model.TradeType;

import java.time.LocalDateTime;

public class StockTradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByUserId(Long userId);
    List<Trade> findByTradeType(TradeType tradeType);
    List<Trade> findByStatus(TradeStatus status);
    List<Trade> findBySymbolIgnoreCase(String symbol);
    List<Trade> findByExecutionBetween(LocalDateTime start, LocalDateTime end);
}
