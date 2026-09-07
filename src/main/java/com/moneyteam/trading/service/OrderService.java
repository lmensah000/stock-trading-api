package com.moneyteam.trading.service;

import com.moneyteam.trading.dto.OrderRequestDto;
import com.moneyteam.trading.dto.OrderResponseDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Every operation takes the acting user's id, sourced from the authenticated
 * principal. Orders belonging to other users are treated as not found.
 */
public interface OrderService {

    OrderResponseDto placeOrder(Long userId, OrderRequestDto request);

    Optional<OrderResponseDto> getById(Long userId, Long id);

    List<OrderResponseDto> listByUser(Long userId);

    OrderResponseDto fill(Long userId, Long orderId, double filledQuantity, BigDecimal fillPrice);

    OrderResponseDto cancel(Long userId, Long orderId);

    OrderResponseDto reject(Long userId, Long orderId);
}
