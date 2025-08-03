package com.moneyteam.service;

import com.moneyteam.model.Options;
import com.moneyteam.model.StockTradeRequest;
import com.moneyteam.model.User;

public interface StockTradingService {
    void executeTrade(StockTradeRequest tradeRequest, PurchaseTrade stockPurchaseTrade );
    void executeOptionsTrade(User user, Options options, StockStrategies strategy);
}

/*


structuring your code in this way, the service layer becomes the central hub for
handling user interactions with the API and performing stock or options trades based on
 strategies, analysis, and risk management. The controller acts as the entry point for user
  requests and delegates the processing to the appropriate methods in the service layer.

aRemember to handle exceptions, validate user input, and implement appropriate security measures
when interacting with the API.
 */