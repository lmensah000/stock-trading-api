package com.moneyteam.repository;

import com.moneyteam.model.Trade;
import com.moneyteam.model.enums.TradeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public class StockTradeRepository implements JpaRepository<Trade, Long> {
    List<Trade> findByUserId(Long userId);
    List<Trade> findByTradeType(TradeType tradeType);
    List<Trade> findByStatus(TradeStatus status);
    List<Trade> findBySymbolIgnoreCase(String symbol);
    List<Trade> findByExecutionBetween(LocalDateTime start, LocalDateTime end);
}
