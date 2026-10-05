package com.moneyteam.alert.dto;

import com.moneyteam.alert.model.Alert;
import com.moneyteam.strategy.model.Confidence;
import com.moneyteam.strategy.model.Direction;
import com.moneyteam.strategy.model.SignalType;

import java.time.Instant;

/** An alert as returned over the API. Carries no user id: the caller is the user. */
public record AlertResponseDto(
        Long id,
        SignalType signalType,
        String stockTicker,
        Direction direction,
        Confidence confidence,
        String summary,
        String evidence,
        Instant detectedAt,
        Instant acknowledgedAt) {

    public static AlertResponseDto from(Alert alert) {
        return new AlertResponseDto(
                alert.getId(),
                alert.getSignalType(),
                alert.getStockTicker(),
                alert.getDirection(),
                alert.getConfidence(),
                alert.getSummary(),
                alert.getEvidence(),
                alert.getDetectedAt(),
                alert.getAcknowledgedAt());
    }
}
