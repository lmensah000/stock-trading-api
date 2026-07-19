package com.moneyteam.trading.service;

import com.moneyteam.trading.dto.TradeRequestDto;
import com.moneyteam.trading.dto.TradeResponseDto;
import com.moneyteam.trading.model.Position;
import com.moneyteam.trading.model.Trade;
import com.moneyteam.trading.model.enums.TradeStatus;
import com.moneyteam.trading.model.enums.OrderSide;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TradeService {
    TradeResponseDto create(TradeRequestDto request);

    Optional<TradeResponseDto> getById(Long id);

    List<TradeResponseDto> listByUser(Long userId);

    List<TradeResponseDto> listBySide(OrderSide side);

    List<TradeResponseDto> listByStatus(TradeStatus status);

    List<TradeResponseDto> listByStockTicker(String stockTicker);

    List<TradeResponseDto> listBetween(LocalDateTime start, LocalDateTime end);

    TradeResponseDto updateStatus(Long id, TradeStatus newStatus);

    TradeResponseDto placeTrade(TradeRequestDto dto);

    List<TradeResponseDto> getTradeHistory(Long userId);

    List<?> getUserPositions(Long userId);

    void updatePosition(Position position, Trade trade);

    void cancelTrade(Long tradeId);
}
