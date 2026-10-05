package com.moneyteam.alert.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneyteam.alert.model.Alert;
import com.moneyteam.alert.repository.AlertRepository;
import com.moneyteam.strategy.model.Signal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Turns signals into stored alerts, suppressing repeats.
 *
 * <h2>Why the cooldown exists</h2>
 * A scan running every five minutes re-detects the same unfilled gap on every
 * pass. Without suppression one condition produces twelve alerts an hour, the
 * feed becomes unreadable, and the feature trains its user to ignore it. The
 * cooldown is what makes this an alerting system rather than a noise generator,
 * so it is part of the core path and not an optional refinement.
 *
 * Suppression is keyed on the signal's {@code dedupeKey}, which identifies the
 * condition rather than the observation, and scoped per user.
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final ObjectMapper objectMapper;
    private final Duration cooldown;

    public AlertService(AlertRepository alertRepository,
                        ObjectMapper objectMapper,
                        @Value("${app.alerts.cooldown-minutes:60}") long cooldownMinutes) {
        this.alertRepository = alertRepository;
        this.objectMapper = objectMapper;
        this.cooldown = Duration.ofMinutes(cooldownMinutes);
    }

    /**
     * Stores a signal as an alert unless the same condition already alerted
     * within the cooldown window.
     *
     * @return the stored alert, or empty when suppressed as a duplicate
     */
    @Transactional
    public Optional<Alert> raise(Long userId, Signal signal) {
        Instant since = signal.detectedAt().minus(cooldown);

        if (alertRepository.existsByUserIdAndDedupeKeyAndDetectedAtAfter(userId, signal.dedupeKey(), since)) {
            log.debug("Suppressed duplicate alert for user {} within cooldown: {}", userId, signal.dedupeKey());
            return Optional.empty();
        }

        Alert alert = new Alert();
        alert.setUserId(userId);
        alert.setSignalType(signal.type());
        alert.setStockTicker(signal.ticker());
        alert.setDirection(signal.direction());
        alert.setConfidence(signal.confidence());
        alert.setSummary(signal.summary());
        alert.setEvidence(serialiseEvidence(signal));
        alert.setDedupeKey(signal.dedupeKey());
        alert.setDetectedAt(signal.detectedAt());

        return Optional.of(alertRepository.save(alert));
    }

    /** Raises a batch, returning only those actually stored. */
    @Transactional
    public List<Alert> raiseAll(Long userId, List<Signal> signals) {
        List<Alert> stored = new ArrayList<>();
        for (Signal signal : signals) {
            raise(userId, signal).ifPresent(stored::add);
        }
        return stored;
    }

    @Transactional(readOnly = true)
    public List<Alert> listForUser(Long userId, boolean unacknowledgedOnly) {
        return unacknowledgedOnly
                ? alertRepository.findByUserIdAndAcknowledgedAtIsNullOrderByDetectedAtDesc(userId)
                : alertRepository.findByUserIdOrderByDetectedAtDesc(userId);
    }

    /**
     * Marks an alert acknowledged. Another user's alert reports not-found
     * rather than forbidden, so ids cannot be probed.
     */
    @Transactional
    public Alert acknowledge(Long userId, Long alertId) {
        Alert alert = alertRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new NoSuchElementException("Alert not found: " + alertId));

        if (!alert.isAcknowledged()) {
            alert.setAcknowledgedAt(Instant.now());
        }
        return alertRepository.save(alert);
    }

    private String serialiseEvidence(Signal signal) {
        try {
            return objectMapper.writeValueAsString(signal.evidence());
        } catch (JsonProcessingException e) {
            // Evidence is valuable but not worth losing the alert over.
            log.warn("Could not serialise evidence for {}; storing alert without it", signal.dedupeKey(), e);
            return "{}";
        }
    }
}
