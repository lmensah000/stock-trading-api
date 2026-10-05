package com.moneyteam.strategy.model;

/** What kind of condition fired. */
public enum SignalType {

    /** The underlying moved by at least a configured percentage. */
    PERCENT_MOVE,

    /** Option volume is unusually high relative to open interest. */
    UNUSUAL_OPTION_VOLUME,

    /** Unusual option volume corroborated by Fair Value Gaps across timeframes. */
    HIGH_VALUE_OPTION
}
