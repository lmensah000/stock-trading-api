package com.moneyteam.repository;

import com.moneyteam.model.Stock;
import com.moneyteam.model.Trade;
import com.moneyteam.model.enums.TradeStatus;
import com.moneyteam.model.enums.TradeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StockTradeRepository extends JpaRepository<Stock, Long> {

    Stock findByStockTicker(String stockTicker);
    List<Stock> findBySector(String sector);
}
