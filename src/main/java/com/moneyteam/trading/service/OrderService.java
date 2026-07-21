package com.moneyteam.trading.service;

import com.moneyteam.trading.dto.OrderRequestDto;
import com.moneyteam.trading.dto.OrderResponseDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderService {

    OrderResponseDto placeOrder(OrderRequestDto request);

    Optional<OrderResponseDto> getById(Long id);

    List<OrderResponseDto> listByUser(Long userId);

    OrderResponseDto fill(Long orderId, double filledQuantity, BigDecimal fillPrice);

    OrderResponseDto cancel(Long orderId);

    OrderResponseDto reject(Long orderId);
}
