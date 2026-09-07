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

/**
 * Every operation that touches a user's data takes the acting user's id as an
 * explicit parameter. Callers must source it from the authenticated principal,
 * never from client-supplied request data.
 */
public interface TradeService {
    TradeResponseDto create(Long userId, TradeRequestDto request);

    Optional<TradeResponseDto> getById(Long userId, Long id);

    List<TradeResponseDto> listByUser(Long userId);

    List<TradeResponseDto> listBySide(Long userId, OrderSide side);

    List<TradeResponseDto> listByStatus(Long userId, TradeStatus status);

    List<TradeResponseDto> listByStockTicker(Long userId, String stockTicker);

    List<TradeResponseDto> listBetween(Long userId, LocalDateTime start, LocalDateTime end);

    TradeResponseDto updateStatus(Long userId, Long id, TradeStatus newStatus);

    TradeResponseDto placeTrade(Long userId, TradeRequestDto dto);

    List<TradeResponseDto> getTradeHistory(Long userId);

    List<Position> getUserPositions(Long userId);

    void updatePosition(Position position, Trade trade);

    void cancelTrade(Long userId, Long tradeId);
}
