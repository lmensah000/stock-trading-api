package com.moneyteam.trading.controller;

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

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public ResponseEntity<OrderResponseDto> placeOrder(@Valid @RequestBody OrderRequestDto dto) {
        log.info("Placing order for stockTicker: {}", dto.getStockTicker());
        return ResponseEntity.ok(orderService.placeOrder(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getById(@PathVariable Long id) {
        return orderService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponseDto>> listByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.listByUser(userId));
    }

    @PostMapping("/{id}/fill")
    public ResponseEntity<OrderResponseDto> fill(@PathVariable Long id,
                                                  @RequestParam @Positive double quantity,
                                                  @RequestParam BigDecimal price) {
        return ResponseEntity.ok(orderService.fill(id, quantity, price));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponseDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancel(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<OrderResponseDto> reject(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.reject(id));
    }
}
