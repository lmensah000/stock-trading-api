package com.moneyteam.marketdata.repository;

import com.moneyteam.marketdata.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StockTradeRepository extends JpaRepository<Stock, Long> {

    Stock findByStockTicker(String stockTicker);
    List<Stock> findBySector(String sector);
}
