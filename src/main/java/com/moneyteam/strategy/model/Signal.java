package com.moneyteam.strategy.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * What a rule emits when its condition is met. Immutable.
 *
 * <h2>Evidence</h2>
 * {@code evidence} carries the numbers that justified the signal - the measured
 * ratio, the threshold it cleared, which timeframes agreed, how stale the open
 * interest was. This is not decoration. An alert that says only "unusual
 * activity in NVDA" cannot be reviewed afterwards; one that records the figures
 * it fired on can be, and reviewing losing signals honestly is the only way
 * thresholds ever improve.
 *
 * <h2>Deduplication</h2>
 * {@code dedupeKey} identifies the underlying condition rather than this
 * observation of it. A scan running every five minutes re-detects the same gap
 * repeatedly; without a stable key per condition the alert feed becomes noise
 * within an hour.
 *
 * <p>This type is deliberately free of any persistence or framework annotation.
 * The strategy package stays pure logic; turning a signal into a stored alert
 * is the alert package's job.
 */
public record Signal(
        SignalType type,
        String ticker,
        Direction direction,
        Confidence confidence,
        Instant detectedAt,
        String summary,
        String dedupeKey,
        Map<String, String> evidence) {

    public Signal {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(ticker, "ticker");
        Objects.requireNonNull(confidence, "confidence");
        Objects.requireNonNull(detectedAt, "detectedAt");
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(dedupeKey, "dedupeKey");

        // Sorted and unmodifiable: sorted so evidence renders and compares
        // deterministically, unmodifiable so a Signal handed to several threads
        // cannot be mutated by any of them.
        evidence = Collections.unmodifiableMap(
                new TreeMap<>(evidence == null ? Map.of() : evidence));
    }
}
