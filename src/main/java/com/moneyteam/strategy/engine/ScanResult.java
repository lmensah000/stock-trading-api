package com.moneyteam.strategy.engine;

import com.moneyteam.strategy.model.Signal;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * The outcome of one scan across a universe.
 *
 * <h2>Failures are returned, not swallowed</h2>
 * {@code failures} lists every ticker that could not be evaluated and why. This
 * is deliberate and load-bearing: a scan that quietly covered 400 of 500 names
 * looks identical to one that covered all 500, and the difference is whether
 * the names that failed were the ones that mattered. A caller that wants to
 * ignore failures must do so explicitly.
 *
 * @param signals    signals found, in deterministic order
 * @param failures   ticker to failure reason, for every ticker that errored or timed out
 * @param scanned    how many tickers were attempted
 * @param elapsed    wall-clock duration of the scan
 */
public record ScanResult(
        List<Signal> signals,
        Map<String, String> failures,
        int scanned,
        Duration elapsed) {

    public ScanResult {
        signals = List.copyOf(signals);
        failures = Map.copyOf(failures);
    }

    public boolean hasFailures() {
        return !failures.isEmpty();
    }

    /** Fraction of attempted tickers that were evaluated without error. */
    public double completeness() {
        return scanned == 0 ? 1.0 : (double) (scanned - failures.size()) / scanned;
    }
}
