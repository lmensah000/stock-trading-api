package com.moneyteam.controller;

import com.moneyteam.dto.TradeRequestDto;
import com.moneyteam.dto.TradeResponseDto;
import com.moneyteam.model.enums.TradeStatus;
import com.moneyteam.model.enums.TradeType;
import com.moneyteam.service.TradeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeService tradeService;

    @Autowired
    public TradeController(TradeService tradeService)
    { this.tradeService = tradeService; }

    @PostMapping("create")
    public ResponseEntity<TradeResponseDto> create(@Valid @RequestBody TradeRequestDto dto) {
        log.info("Received trade creation request for symbol: {}", dto.getSymbol());
        TradeResponseDto response = tradeService.create(dto);
        log.info("Trade successfully created: {}", response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TradeResponseDto> getById(@PathVariable Long id) {
        log.info("Fetching trade by ID: {}", id);
        return tradeService.getById(id)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) TradeType tradeType,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) TradeStatus status,
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end) {

        log.info("Search request - userId={}, type={}, symbol={}, status={}, start={}, end={}",
                userId, tradeType, symbol, status, start, end);

                if (userId != null)
                    return ResponseEntity.ok(tradeService.listByUser((userId)));
                if (tradeType != null)
                    return ResponseEntity.ok(tradeService.listByType(tradeType));
                if (symbol != null)
                    return ResponseEntity.ok(tradeService.listBySymbol(symbol));
                if (status != null)
                    return ResponseEntity.ok(tradeService.listByStatus(status));
                if (status != null)
                    return ResponseEntity.ok(tradeService.listByStatus(status));
                if (start != null && end != null)
                    return ResponseEntity.ok(tradeService.listBetween(start, end));

            log.warn("Search called without parameters.");
            return ResponseEntity.badRequest().body("Please provide at least one filter parameter.");

    }
}
