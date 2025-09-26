package com.moneyteam.mapper;

import com.moneyteam.dto.TradeRequestDto;
import com.moneyteam.dto.TradeResponseDto;

public class TradeMapper {

    private TradeMapper() {}

    public static Trade toEntity(TradeRequestDto dto) {
        Trade t = new Trade();
        t.setTradeType(dto.getTradeType());
        t.setSymbol(dto.getSymbol());
        t.setQuantity(dto.getQuanitty());
        t.setPrice(dto.getPrice());
        t.setExecutionDate(dto.getExecutionDate());
        t.setStatus(TradeStatus.PENDING);
        t.setUserId(dto.getUserId());
        return t;
    }

    public static TradeResponseDto toDto(Trade trade) {
        TradeResponseDto dto = new TradeResponseDto();
        dto.setId(trade.getId());
        dto.setSymbol(trade.getSymbol());
        dto.setQuanitty((trade.getQuantity));
        dto.setPrice(trade.getPrice());
        dto.setExecutionDate(trade.getExecutionDate());
        dto.setStatus(trade.getStatus());
        dto.setUserId(trade.getUserId());
        return dto;
    }
}
