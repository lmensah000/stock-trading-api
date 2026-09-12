package com.moneyteam.trading.controller;

import com.moneyteam.common.security.CurrentUserService;
import com.moneyteam.trading.dto.TradeRequestDto;
import com.moneyteam.trading.dto.TradeResponseDto;
import com.moneyteam.trading.model.enums.TradeStatus;
import com.moneyteam.trading.model.enums.OrderSide;
import com.moneyteam.trading.service.TradeService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

//— includes endpoints
//✔ /trade/place
//✔ /trade/history/{userId}
//✔ /trade/positions/{userId}
//✔ /trade/{tradeId}
//✔ /trade/cancel/{tradeId}

@RestController
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeService tradeService;
    private final CurrentUserService currentUserService;

    @Autowired
    public TradeController(TradeService tradeService, CurrentUserService currentUserService) {
        this.tradeService = tradeService;
        this.currentUserService = currentUserService;
    }

    private static final Logger log = LoggerFactory.getLogger(TradeController.class);

    @PostMapping("/create")
    public ResponseEntity<TradeResponseDto> create(@Valid @RequestBody TradeRequestDto dto) {
        log.info("Received trade creation request for stockTicker: {}", dto.getStockTicker());
        TradeResponseDto response = tradeService.create(currentUserService.getCurrentUserId(), dto);
        log.info("Trade successfully created: {}", response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TradeResponseDto> getById(@PathVariable Long id) {
        log.info("Fetching trade by ID: {}", id);
        return tradeService.getById(currentUserService.getCurrentUserId(), id)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Searches the caller's own trades. The user is never a query parameter -
     * results are always scoped to the authenticated principal.
     */
    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam(required = false) OrderSide side,
            @RequestParam(required = false) String stockTicker,
            @RequestParam(required = false) TradeStatus status,
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end) {

        Long userId = currentUserService.getCurrentUserId();
        log.info("Search request - side={}, stockTicker={}, status={}, start={}, end={}",
                side, stockTicker, status, start, end);

        if (side != null)
            return ResponseEntity.ok(tradeService.listBySide(userId, side));
        if (stockTicker != null)
            return ResponseEntity.ok(tradeService.listByStockTicker(userId, stockTicker));
        if (status != null)
            return ResponseEntity.ok(tradeService.listByStatus(userId, status));
        if (start != null && end != null)
            return ResponseEntity.ok(tradeService.listBetween(userId, start, end));

        // No filter given: return the caller's full history rather than an error.
        return ResponseEntity.ok(tradeService.listByUser(userId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        tradeService.cancelTrade(currentUserService.getCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
