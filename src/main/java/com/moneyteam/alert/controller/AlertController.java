package com.moneyteam.alert.controller;

import com.moneyteam.alert.dto.AlertResponseDto;
import com.moneyteam.alert.service.AlertService;
import com.moneyteam.common.security.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The caller's own alerts.
 *
 * As with every other trading route, identity comes from the authenticated
 * principal and never from a path variable or request body.
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;
    private final CurrentUserService currentUserService;

    public AlertController(AlertService alertService, CurrentUserService currentUserService) {
        this.alertService = alertService;
        this.currentUserService = currentUserService;
    }

    /**
     * @param unacknowledgedOnly when true, returns only alerts not yet acknowledged
     */
    @GetMapping
    public ResponseEntity<List<AlertResponseDto>> list(
            @RequestParam(defaultValue = "false") boolean unacknowledgedOnly) {

        List<AlertResponseDto> alerts = alertService
                .listForUser(currentUserService.getCurrentUserId(), unacknowledgedOnly)
                .stream()
                .map(AlertResponseDto::from)
                .toList();

        return ResponseEntity.ok(alerts);
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<AlertResponseDto> acknowledge(@PathVariable Long id) {
        return ResponseEntity.ok(AlertResponseDto.from(
                alertService.acknowledge(currentUserService.getCurrentUserId(), id)));
    }
}
