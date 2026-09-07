package com.moneyteam.marketdata.repository;

import com.moneyteam.marketdata.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

// Stock's @Id is the String ticker, so the id type is String. Declaring Long
// here mismatched the entity and broke id-based repository operations.
public interface StockTradeRepository extends JpaRepository<Stock, String> {

    Stock findByStockTicker(String stockTicker);
    List<Stock> findBySector(String sector);
}
