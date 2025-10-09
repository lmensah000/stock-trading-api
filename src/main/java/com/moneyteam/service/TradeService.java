package com.moneyteam.service;

import com.moneyteam.dto.TradeRequestDto;
import com.moneyteam.dto.TradeResponseDto;
import com.moneyteam.model.enums.TradeStatus;
import com.moneyteam.model.enums.TradeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TradeService {
    TradeResponseDto create(TradeRequestDto request);
    Optional<TradeResponseDto> getById(Long id);
    List<TradeResponseDto> listByUser(Long userId);
    List<TradeResponseDto> listByType(TradeType tradeType);
    List<TradeResponseDto> listBySymbol(String symbol);
    List<TradeResponseDto> listBetween(LocalDateTime start, LocalDateTime end);
    TradeResponseDto updateStatus(Long id, TradeStatus newStatus);
}
