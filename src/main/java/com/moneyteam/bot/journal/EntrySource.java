package com.moneyteam.bot.journal;

/** Where a journal entry came from. */
public enum EntrySource {

    /**
     * Parsed from a broker trade-history export. Gives history immediately,
     * but the option chain as it stood at the fill is gone - historical chains
     * carrying open interest are not freely available.
     */
    BROKER_IMPORT,

    /**
     * Recorded as the trade was made, so the full market context could be
     * snapshotted. The only way to capture chain context at all.
     */
    LIVE_CAPTURE
}
