package com.moneyteam.repository;

import com.moneyteam.model.Trade;
import com.moneyteam.model.enums.TradeStatus;
import com.moneyteam.model.enums.TradeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByUserId(Long userId);

    List<Trade> findByTradeType(TradeType tradeType);

    List<Trade> findByStatus(TradeStatus status);

    List<Trade> findBySymbolIgnoreCase(String symbol);

    List<Trade> findByExecutionDateBetween(LocalDateTime start, LocalDateTime end);
}