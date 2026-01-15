package com.moneyteam.mapper;

import com.moneyteam.dto.TradeRequestDto;
import com.moneyteam.dto.TradeResponseDto;
import com.moneyteam.model.Trade;
import com.moneyteam.model.enums.TradeStatus;

import java.time.LocalDateTime;

/**
 * Mapper class for converting between Trade entity and DTOs.
 */
public class TradeMapper {

    private TradeMapper() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts a TradeRequestDto to a Trade entity.
     *
     * @param dto The TradeRequestDto to convert
     * @return The Trade entity
     */
    public static Trade toEntity(TradeRequestDto dto) {
        if (dto == null) {
            return null;
        }

        Trade trade = new Trade();
        trade.setStockTicker(dto.getStockTicker());
        trade.setQuantity(dto.getQuantity());
        trade.setPrice(dto.getPrice());
        trade.setTradeType(dto.getTradeType());
        trade.setStatus(dto.getStatus() != null ? dto.getStatus() : TradeStatus.PENDING);
        trade.setExecutionDate(dto.getExecutionDate() != null ? dto.getExecutionDate() : LocalDateTime.now());
        trade.setUserTradeId(dto.getUserId());

        return trade;
    }

    /**
     * Converts a Trade entity to a TradeResponseDto.
     *
     * @param trade The Trade entity to convert
     * @return The TradeResponseDto
     */
    public static TradeResponseDto toDto(Trade trade) {
        if (trade == null) {
            return null;
        }

        TradeResponseDto dto = new TradeResponseDto();
        dto.setId(trade.getId());
        dto.setStockTicker(trade.getStockTicker());
        dto.setQuantity(trade.getQuantity());
        dto.setPrice(trade.getPrice());
        dto.setTradeType(trade.getTradeType());
        dto.setStatus(trade.getStatus());
        dto.setExecutionDate(trade.getExecutionDate());
        dto.setUserId(trade.getUserTradeId());

        return dto;
    }

    /**
     * Updates an existing Trade entity with values from a TradeRequestDto.
     *
     * @param trade The Trade entity to update
     * @param dto The TradeRequestDto with new values
     */
    public static void updateEntity(Trade trade, TradeRequestDto dto) {
        if (trade == null || dto == null) {
            return;
        }

        if (dto.getStockTicker() != null) {
            trade.setStockTicker(dto.getStockTicker());
        }
        if (dto.getQuantity() != null) {
            trade.setQuantity(dto.getQuantity());
        }
        if (dto.getPrice() != null) {
            trade.setPrice(dto.getPrice());
        }
        if (dto.getTradeType() != null) {
            trade.setTradeType(dto.getTradeType());
        }
        if (dto.getStatus() != null) {
            trade.setStatus(dto.getStatus());
        }
        if (dto.getExecutionDate() != null) {
            trade.setExecutionDate(dto.getExecutionDate());
        }
    }
}
