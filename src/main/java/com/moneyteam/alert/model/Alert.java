package com.moneyteam.alert.model;

import com.moneyteam.strategy.model.Confidence;
import com.moneyteam.strategy.model.Direction;
import com.moneyteam.strategy.model.SignalType;

import javax.persistence.*;
import java.time.Instant;

/**
 * A signal persisted for a user to act on.
 *
 * This is the boundary between the pure strategy package and the application:
 * {@code Signal} is a value with no framework coupling, {@code Alert} is its
 * stored, user-scoped form.
 *
 * {@code dedupeKey} identifies the underlying condition rather than the
 * observation, so repeated scans of the same unchanged condition collapse into
 * one alert instead of one per scan.
 */
@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alert_user", columnList = "user_id"),
        @Index(name = "idx_alert_dedupe", columnList = "user_id,dedupe_key,detected_at")
})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false)
    private SignalType signalType;

    @Column(name = "stock_ticker", nullable = false)
    private String stockTicker;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction")
    private Direction direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence", nullable = false)
    private Confidence confidence;

    @Column(name = "summary", nullable = false, length = 512)
    private String summary;

    /**
     * The figures the signal fired on, serialised as JSON. Stored so a signal
     * can be reviewed after the fact - an alert that cannot be audited cannot
     * be used to improve the threshold that produced it.
     */
    @Lob
    @Column(name = "evidence")
    private String evidence;

    @Column(name = "dedupe_key", nullable = false, length = 255)
    private String dedupeKey;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public SignalType getSignalType() {
        return signalType;
    }

    public void setSignalType(SignalType signalType) {
        this.signalType = signalType;
    }

    public String getStockTicker() {
        return stockTicker;
    }

    public void setStockTicker(String stockTicker) {
        this.stockTicker = stockTicker;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Confidence getConfidence() {
        return confidence;
    }

    public void setConfidence(Confidence confidence) {
        this.confidence = confidence;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getDedupeKey() {
        return dedupeKey;
    }

    public void setDedupeKey(String dedupeKey) {
        this.dedupeKey = dedupeKey;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public boolean isAcknowledged() {
        return acknowledgedAt != null;
    }
}
