package com.moneyteam.bot;

/**
 * Whether the bot is simulating or trading real money.
 *
 * There are deliberately only two values. A third, such as a "live but
 * restricted" mode, would create a state where the answer to "can this place a
 * real order" is neither plainly yes nor plainly no, and that is the one
 * question this enum exists to answer unambiguously.
 */
public enum TradingMode {

    /**
     * Orders are simulated against the in-app account and ledger. Nothing
     * reaches a broker. This is the default and the only mode with a working
     * execution path today.
     */
    PAPER,

    /**
     * Real money. Reachable only after the paper equity target has been met
     * and the owner has explicitly unlocked it.
     *
     * No live broker adapter exists, so selecting this mode currently grants
     * no new capability - it is the precondition for ever building one, not a
     * switch that starts live trading.
     */
    LIVE
}
