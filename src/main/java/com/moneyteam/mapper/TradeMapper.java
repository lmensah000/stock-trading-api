package com.moneyteam.mapper;

import com.moneyteam.dto.TradeRequestDto;
import com.moneyteam.dto.TradeResponseDto;
import com.moneyteam.model.Trade;

public class TradeMapper {

    private TradeMapper() {}

    public static Trade toEntity(TradeRequestDto dto) {
        Trade t = new Trade();
        t.setTradeType(dto.getTradeType());
        t.setStockTicker(dto.getStockTicker());
        t.setQuantity(dto.getQuantity());
        t.setPrice(dto.getPrice());
        t.setExecutionDate(dto.getExecutionDate());
        t.setStatus(dto.getStatus());
//        t.setUserId(dto.getUserId());
        return t;
    }

    public static TradeResponseDto toDto(Trade trade) {
        TradeResponseDto dto = new TradeResponseDto();
        dto.setId(trade.getId());
        dto.setStockTicker(trade.getStockTicker());
        dto.setQuantity(trade.getQuantity());
        dto.setPrice(trade.getPrice());
        dto.setExecutionDate(trade.getExecutionDate());
        dto.setStatus(trade.getStatus());
//        dto.setUserId(trade.getUserId());
        return dto;
    }

}
