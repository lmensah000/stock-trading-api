package com.moneyteam.strategy.model;

/**
 * How much weight a signal deserves.
 *
 * This is not a probability and must never be treated as one. It records how
 * much corroboration a signal had - how many timeframes agreed, and whether the
 * evidence came from a liquid chart or a thin one.
 */
public enum Confidence {

    /** Corroborated across multiple timeframes on liquid data. */
    HIGH,

    /** Some corroboration, or strong evidence on a single timeframe. */
    MEDIUM,

    /**
     * Weak or structurally unreliable evidence. Every signal derived only from
     * an option contract's own candles lands here, regardless of how clean the
     * pattern looks - see {@link GapSource#OPTION_CONTRACT}.
     */
    LOW;

    /** The weaker of two confidences, used when combining evidence. */
    public Confidence weakest(Confidence other) {
        return compareTo(other) >= 0 ? this : other;
    }
}
