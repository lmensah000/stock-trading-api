package com.moneyteam.repository;

import com.moneyteam.model.PurchaseTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for stock trade (PurchaseTrade) entity operations.
 */
@Repository
public interface StockTradeRepository extends JpaRepository<PurchaseTrade, Long> {

    List<PurchaseTrade> findByUserId(Long userId);

    List<PurchaseTrade> findByStockTicker(String stockTicker);

    List<PurchaseTrade> findByUserIdAndStockTicker(Long userId, String stockTicker);

    List<PurchaseTrade> findByExecutionTimeBetween(LocalDateTime start, LocalDateTime end);

    List<PurchaseTrade> findByUserIdOrderByExecutionTimeDesc(Long userId);
}
