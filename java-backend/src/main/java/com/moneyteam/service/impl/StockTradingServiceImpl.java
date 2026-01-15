package com.moneyteam.service.impl;

import com.moneyteam.model.*;
import com.moneyteam.model.enums.TradeStatus;
import com.moneyteam.model.enums.TradeType;
import com.moneyteam.repository.*;
import com.moneyteam.service.StockStrategies;
import com.moneyteam.service.StockTradingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Implementation of StockTradingService for executing stock and options trades.
 */
@Service
@Transactional
public class StockTradingServiceImpl implements StockTradingService {

    private final UserRepository userRepository;
    private final TradeRepository tradeRepository;
    private final PositionRepository positionRepository;
    private final OptionsTradeRepository optionsTradeRepository;

    @Autowired
    public StockTradingServiceImpl(
            UserRepository userRepository,
            TradeRepository tradeRepository,
            PositionRepository positionRepository,
            OptionsTradeRepository optionsTradeRepository
    ) {
        this.userRepository = userRepository;
        this.tradeRepository = tradeRepository;
        this.positionRepository = positionRepository;
        this.optionsTradeRepository = optionsTradeRepository;
    }

    @Override
    @Transactional
    public void executeTrade(StockTradeRequest tradeRequest, PurchaseTrade stockPurchaseTrade) {
        // Validate trade request
        if (tradeRequest == null) {
            throw new IllegalArgumentException("Trade request cannot be null");
        }

        if (tradeRequest.getStockTicker() == null || tradeRequest.getStockTicker().isEmpty()) {
            throw new IllegalArgumentException("Stock ticker is required");
        }

        if (tradeRequest.getQuantity() == null || tradeRequest.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        // Calculate total amount
        BigDecimal stockPrice = stockPurchaseTrade.getStockPrice();
        double quantity = tradeRequest.getQuantity();
        BigDecimal totalAmount = stockPrice.multiply(BigDecimal.valueOf(quantity));

        String tradeType = tradeRequest.getTradeType();
        String summary;

        if ("BUY".equalsIgnoreCase(tradeType)) {
            summary = String.format("Successfully bought %.2f shares of %s @ total of $%.2f",
                    quantity, tradeRequest.getStockTicker(), totalAmount);
        } else if ("SELL".equalsIgnoreCase(tradeType)) {
            summary = String.format("Successfully sold %.2f shares of %s @ $%.2f",
                    quantity, tradeRequest.getStockTicker(), totalAmount);
        } else {
            throw new IllegalArgumentException("Invalid trade type: " + tradeType);
        }

        // Update the purchase trade with execution details
        stockPurchaseTrade.setStockTicker(tradeRequest.getStockTicker());
        stockPurchaseTrade.setQuantity(quantity);
        stockPurchaseTrade.setTotalAmount(totalAmount);
        stockPurchaseTrade.setExecutionTime(LocalDateTime.now());
        stockPurchaseTrade.setUserId(tradeRequest.getUserId());

        System.out.println(summary);
    }

    @Override
    @Transactional
    public Trade executeTrade(Long userId, Trade trade) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        trade.setUsers(user);
        trade.setUserTradeId(userId);
        trade.setExecutionDate(LocalDateTime.now());
        trade.setStatus(TradeStatus.PENDING);

        // Validate and execute
        validateTrade(trade);
        trade.setStatus(TradeStatus.EXECUTED);

        return tradeRepository.save(trade);
    }

    @Override
    @Transactional
    public Trade cancelTrade(Long tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new RuntimeException("Trade not found: " + tradeId));

        if (trade.getStatus() == TradeStatus.EXECUTED) {
            throw new IllegalStateException("Cannot cancel an executed trade");
        }

        trade.setStatus(TradeStatus.CANCELLED);
        return tradeRepository.save(trade);
    }

    @Override
    @Transactional
    public void executeOptionsTrade(User user, Options options, StockStrategies strategy) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (options == null) {
            throw new IllegalArgumentException("Options cannot be null");
        }

        if (strategy == null) {
            throw new IllegalArgumentException("Strategy cannot be null");
        }

        // Create a trade record for the options trade
        Trade trade = new Trade();
        trade.setUsers(user);
        trade.setUserTradeId(user.getId());
        trade.setStockTicker(options.getStockTicker());
        trade.setTradeType(TradeType.BUY); // Options trades default to BUY
        trade.setStatus(TradeStatus.PENDING);
        trade.setExecutionDate(LocalDateTime.now());

        // Save the trade
        Trade savedTrade = tradeRepository.save(trade);

        // Create option trade details
        OptionTradeDetails optionDetails = new OptionTradeDetails();
        optionDetails.setTrade(savedTrade);
        optionDetails.setOptionType(options.getOptionType());
        optionDetails.setStrikePrice(options.getStrikePrice());
        optionDetails.setUnderlyingTicker(options.getStockTicker());

        // Save option details
        optionsTradeRepository.save(optionDetails);

        // Update trade status
        savedTrade.setStatus(TradeStatus.EXECUTED);
        tradeRepository.save(savedTrade);

        System.out.println("Options trade executed successfully using strategy: " + strategy.getName());
    }

    private void validateTrade(Trade trade) {
        if (trade.getStockTicker() == null || trade.getStockTicker().isEmpty()) {
            throw new IllegalArgumentException("Stock ticker is required");
        }

        if (trade.getQuantity() == null || trade.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (trade.getTradeType() == null) {
            throw new IllegalArgumentException("Trade type is required");
        }
    }
}
