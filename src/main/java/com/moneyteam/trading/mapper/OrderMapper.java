package com.moneyteam.trading.mapper;

import com.moneyteam.trading.dto.OrderRequestDto;
import com.moneyteam.trading.dto.OrderResponseDto;
import com.moneyteam.trading.model.Order;
import com.moneyteam.trading.model.enums.OrderStatus;

import java.time.LocalDateTime;

public class OrderMapper {

    private OrderMapper() {}

    public static Order toEntity(OrderRequestDto dto) {
        Order order = new Order();
        order.setUserId(dto.getUserId());
        order.setStockTicker(dto.getStockTicker());
        order.setQuantity(dto.getQuantity());
        order.setTargetPrice(dto.getTargetPrice());
        order.setOrderType(dto.getOrderType());
        order.setSide(dto.getSide());
        order.setStatus(OrderStatus.NEW);
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }

    public static OrderResponseDto toDto(Order order) {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setStockTicker(order.getStockTicker());
        dto.setQuantity(order.getQuantity());
        dto.setFilledQuantity(order.getFilledQuantity());
        dto.setTargetPrice(order.getTargetPrice());
        dto.setOrderType(order.getOrderType());
        dto.setSide(order.getSide());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());
        return dto;
    }
}
