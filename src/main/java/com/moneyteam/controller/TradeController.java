package com.moneyteam.controller;

import java.util.List;
import com.moneyteam.dto.TradeRequestDto;
import com.moneyteam.dto.TradeResponseDto;
import com.moneyteam.model.enums.TradeType;
import com.moneyteam.service.TradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeService tradeService;
    public TradeController(TradeService tradeService)
    { this.tradeService = tradeService; }

    @PostMapping
    public ResponseEntity<TradeResponseDto> create(@Valid @RequestBody TradeRequestDto dto) {
        return ResponseEntity.ok(tradeService.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TradeResponseDto> getById(@PathVariable Long id) {
        return tradeService.getById(id)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<TradeResponseDto>> search(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) TradeType tradeType,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end) {

                if (userId != null)
                    return ResponseEntity.ok(tradeService.listByUser((userId)));
                if (tradeType != null)
                    return ResponseEntity.ok(tradeService.listByType(tradeType));
                if (symbol != null)
                    return ResponseEntity.ok(tradeService.listBySymbol(symbol));
                if (status != null)
                    return ResponseEntity.ok(tradeService.listByStatus(tradeType));

        return null;
    }
}
