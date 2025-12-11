package com.moneyteam.service;

import com.moneyteam.model.Options;
import com.moneyteam.model.Stock;
import com.moneyteam.model.User;

public interface StockStrategies {
    void executeStockTrade(User users, Stock stock, StockStrategies strategy);
    void executeOptionsTrade(User users, Options options, StockStrategies strategy);
    // Other methods for analysis, risk management, etc.
}

/*



 */