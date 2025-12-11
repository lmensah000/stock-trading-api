package com.moneyteam.repository;

import com.moneyteam.model.Trade;
import com.moneyteam.model.enums.TradeStatus;
import com.moneyteam.model.enums.TradeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByUserTradeId(@Param("userId") Long userId);

    List<Trade> findByTradeType(TradeType tradeType);

    List<Trade> findByStatus(TradeStatus status);

    List<Trade> findByStockTickerIgnoreCase(String stockTicker);

    List<Trade> findByExecutionDateBetween(LocalDateTime start, LocalDateTime end);
}