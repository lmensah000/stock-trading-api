package com.moneyteam.bot.journal;

/**
 * How much of the market context an entry actually carries.
 *
 * Tracked explicitly because the tiers are not comparable. An analysis that
 * pooled them would weight a record holding only daily bars equally with one
 * holding the full chain, and would then report a confidence the thin records
 * cannot support. Every analysis is scoped to a single tier.
 */
public enum ContextFidelity {

    /** Underlying bars only. What an imported fill can be reconstructed with. */
    UNDERLYING_ONLY,

    /**
     * Underlying bars plus the end-of-day chain for that session. Available
     * from commercial historical options data, and enough for a swing strategy
     * whose entries are daily anyway.
     */
    UNDERLYING_PLUS_EOD_CHAIN,

    /**
     * Everything, captured at the moment of the fill: bars, the contract's own
     * quote, volume, open interest. Only obtainable live.
     */
    FULL_CHAIN_AT_FILL;

    /** True when option-chain features may be read from this entry at all. */
    public boolean hasChainContext() {
        return this != UNDERLYING_ONLY;
    }
}
