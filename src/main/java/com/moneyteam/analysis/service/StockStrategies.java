package com.moneyteam.analysis.service;

import com.moneyteam.trading.model.Options;
import com.moneyteam.marketdata.model.Stock;
import com.moneyteam.user.model.User;

public interface StockStrategies {
    void executeStockTrade(User users, Stock stock, StockStrategies strategy);
    void executeOptionsTrade(User users, Options options, StockStrategies strategy);
    // Other methods for analysis, risk management, etc.
}

/*



 */