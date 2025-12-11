package com.moneyteam.controller;

import com.moneyteam.dto.TradeRequestDto;
import com.moneyteam.dto.TradeResponseDto;
import com.moneyteam.model.enums.TradeStatus;
import com.moneyteam.model.enums.TradeType;
import com.moneyteam.service.TradeService;
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

    @Autowired
    public TradeController(TradeService tradeService)
    { this.tradeService = tradeService; }

    private static final Logger log = LoggerFactory.getLogger(TradeController.class);

    @PostMapping("create")
    public ResponseEntity<TradeResponseDto> create(@Valid @RequestBody TradeRequestDto dto) {
        log.info("Received trade creation request for stockTicker: {}", dto.getStockTicker());
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
            @RequestParam(required = false) Long userTradeId,
            @RequestParam(required = false) TradeType tradeType,
            @RequestParam(required = false) String stockTicker,
            @RequestParam(required = false) TradeStatus status,
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end) {

        log.info("Search request - userId={}, type={}, stockTicker={}, status={}, start={}, end={}",
                userTradeId, tradeType, stockTicker, status, start, end);

                if (userTradeId != null)
                    return ResponseEntity.ok(tradeService.listByUser((userTradeId)));
                if (tradeType != null)
                    return ResponseEntity.ok(tradeService.listByType(tradeType));
                if (stockTicker != null)
                    return ResponseEntity.ok(tradeService.listByStockTicker(stockTicker));
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
