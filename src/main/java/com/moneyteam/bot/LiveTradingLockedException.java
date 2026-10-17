package com.moneyteam.bot;

/**
 * Thrown when something was refused because the live-trading gate is shut, or
 * because the bot has no configuration to decide from.
 *
 * Distinct from a validation failure on purpose: the request was well formed
 * and the caller was authorised. What stopped it was a deliberate safety
 * condition, and that should read differently in a log than a malformed body.
 */
public class LiveTradingLockedException extends RuntimeException {

    public LiveTradingLockedException(String message) {
        super(message);
    }
}
