package com.moneyteam.trading.controller;

import com.moneyteam.common.security.CurrentUserService;
import com.moneyteam.trading.dto.OrderRequestDto;
import com.moneyteam.trading.dto.OrderResponseDto;
import com.moneyteam.trading.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

/**
 * All routes act on the authenticated caller's own orders. There is no
 * caller-supplied user id anywhere in this controller.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    public OrderController(OrderService orderService, CurrentUserService currentUserService) {
        this.orderService = orderService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/create")
    public ResponseEntity<OrderResponseDto> placeOrder(@Valid @RequestBody OrderRequestDto dto) {
        log.info("Placing order for stockTicker: {}", dto.getStockTicker());
        return ResponseEntity.ok(orderService.placeOrder(currentUserService.getCurrentUserId(), dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getById(@PathVariable Long id) {
        return orderService.getById(currentUserService.getCurrentUserId(), id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> listMine() {
        return ResponseEntity.ok(orderService.listByUser(currentUserService.getCurrentUserId()));
    }

    @PostMapping("/{id}/fill")
    public ResponseEntity<OrderResponseDto> fill(@PathVariable Long id,
                                                 @RequestParam @Positive double quantity,
                                                 @RequestParam BigDecimal price) {
        return ResponseEntity.ok(
                orderService.fill(currentUserService.getCurrentUserId(), id, quantity, price));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponseDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancel(currentUserService.getCurrentUserId(), id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<OrderResponseDto> reject(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.reject(currentUserService.getCurrentUserId(), id));
    }
}
