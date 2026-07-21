package com.moneyteam.trading.service.impl;

import com.moneyteam.trading.dto.OrderRequestDto;
import com.moneyteam.trading.dto.OrderResponseDto;
import com.moneyteam.trading.dto.TradeRequestDto;
import com.moneyteam.trading.mapper.OrderMapper;
import com.moneyteam.trading.model.Order;
import com.moneyteam.trading.model.OrderExecution;
import com.moneyteam.trading.model.enums.OrderStatus;
import com.moneyteam.trading.model.enums.TradeStatus;
import com.moneyteam.trading.repository.OrderRepository;
import com.moneyteam.trading.service.OrderService;
import com.moneyteam.trading.service.TradeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Map<OrderStatus, Set<OrderStatus>> LEGAL_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        LEGAL_TRANSITIONS.put(OrderStatus.NEW, EnumSet.of(OrderStatus.PENDING, OrderStatus.CANCELLED, OrderStatus.REJECTED));
        LEGAL_TRANSITIONS.put(OrderStatus.PENDING, EnumSet.of(OrderStatus.PARTIALLY_FILLED, OrderStatus.FILLED,
                OrderStatus.CANCELLED, OrderStatus.REJECTED, OrderStatus.EXPIRED));
        LEGAL_TRANSITIONS.put(OrderStatus.PARTIALLY_FILLED, EnumSet.of(OrderStatus.PARTIALLY_FILLED, OrderStatus.FILLED,
                OrderStatus.CANCELLED, OrderStatus.EXPIRED));
        LEGAL_TRANSITIONS.put(OrderStatus.FILLED, EnumSet.noneOf(OrderStatus.class));
        LEGAL_TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        LEGAL_TRANSITIONS.put(OrderStatus.REJECTED, EnumSet.noneOf(OrderStatus.class));
        LEGAL_TRANSITIONS.put(OrderStatus.EXPIRED, EnumSet.noneOf(OrderStatus.class));
    }

    private final OrderRepository orderRepository;
    private final TradeService tradeService;

    public OrderServiceImpl(OrderRepository orderRepository, TradeService tradeService) {
        this.orderRepository = orderRepository;
        this.tradeService = tradeService;
    }

    private static void transition(Order order, OrderStatus newStatus) {
        Set<OrderStatus> allowed = LEGAL_TRANSITIONS.get(order.getStatus());
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition order " + order.getId() + " from " + order.getStatus() + " to " + newStatus);
        }
        order.setStatus(newStatus);
    }

    @Override
    public OrderResponseDto placeOrder(OrderRequestDto request) {
        Order order = OrderMapper.toEntity(request);
        transition(order, OrderStatus.PENDING);
        return OrderMapper.toDto(orderRepository.save(order));
    }

    @Override
    public Optional<OrderResponseDto> getById(Long id) {
        return orderRepository.findById(id).map(OrderMapper::toDto);
    }

    @Override
    public List<OrderResponseDto> listByUser(Long userId) {
        return orderRepository.findByUserId(userId).stream().map(OrderMapper::toDto).toList();
    }

    @Override
    public OrderResponseDto fill(Long orderId, double filledQuantity, BigDecimal fillPrice) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        double alreadyFilled = order.getFilledQuantity();
        double remaining = order.getQuantity() - alreadyFilled;
        if (filledQuantity <= 0 || filledQuantity > remaining) {
            throw new IllegalArgumentException(
                    "Fill quantity " + filledQuantity + " exceeds remaining order quantity " + remaining);
        }

        OrderExecution execution = new OrderExecution();
        execution.setOrder(order);
        execution.setFilledQuantity(filledQuantity);
        execution.setFillPrice(fillPrice);
        execution.setExecutedAt(LocalDateTime.now());
        order.getExecutions().add(execution);

        double totalFilled = alreadyFilled + filledQuantity;
        transition(order, totalFilled >= order.getQuantity() ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED);
        Order saved = orderRepository.save(order);

        TradeRequestDto tradeRequest = new TradeRequestDto();
        tradeRequest.setUserId(order.getUserId());
        tradeRequest.setStockTicker(order.getStockTicker());
        tradeRequest.setQuantity(filledQuantity);
        tradeRequest.setPrice(fillPrice);
        tradeRequest.setExecutionDate(LocalDateTime.now());
        tradeRequest.setSide(order.getSide());
        tradeRequest.setStatus(TradeStatus.EXECUTED);
        tradeService.placeTrade(tradeRequest);

        return OrderMapper.toDto(saved);
    }

    @Override
    public OrderResponseDto cancel(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        transition(order, OrderStatus.CANCELLED);
        return OrderMapper.toDto(orderRepository.save(order));
    }

    @Override
    public OrderResponseDto reject(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        transition(order, OrderStatus.REJECTED);
        return OrderMapper.toDto(orderRepository.save(order));
    }
}
