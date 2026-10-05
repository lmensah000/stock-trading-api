package com.moneyteam.marketdata.model;

import com.moneyteam.trading.model.OptionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A point-in-time snapshot of one option contract. Immutable.
 *
 * <h2>Why there are two timestamps</h2>
 * {@code asOf} is when the quote and volume were observed. {@code openInterestAsOf}
 * is when the open interest figure was published - and those are not the same
 * moment. OCC publishes open interest once daily, before the next session, so
 * any intraday volume/open-interest ratio compares <em>today's live volume</em>
 * against <em>yesterday's open interest</em>.
 *
 * That is how unusual-options-activity is conventionally measured and it is
 * fine, but it has to be visible rather than implied: a ratio computed from a
 * stale denominator is a different claim than one computed from a live one.
 * Carrying both timestamps means any alert can state how old its denominator
 * was, and matches the staleness discipline the alternative-data work calls for.
 *
 * @param underlying       ticker of the underlying instrument
 * @param optionType       CALL or PUT
 * @param strike           strike price
 * @param expiration       contract expiry, a real date rather than a string
 * @param bid              current best bid
 * @param ask              current best ask
 * @param last             last traded price
 * @param volume           contracts traded in the current session
 * @param openInterest     open contracts as of the last published figure
 * @param asOf             when bid/ask/last/volume were observed
 * @param openInterestAsOf when the open interest figure was published
 */
public record OptionQuote(
        String underlying,
        OptionType optionType,
        BigDecimal strike,
        LocalDate expiration,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal last,
        long volume,
        long openInterest,
        Instant asOf,
        Instant openInterestAsOf) {

    public OptionQuote {
        Objects.requireNonNull(underlying, "underlying");
        Objects.requireNonNull(optionType, "optionType");
        Objects.requireNonNull(strike, "strike");
        Objects.requireNonNull(expiration, "expiration");
        Objects.requireNonNull(asOf, "asOf");
        Objects.requireNonNull(openInterestAsOf, "openInterestAsOf");

        if (volume < 0 || openInterest < 0) {
            throw new IllegalArgumentException(
                    "volume and openInterest cannot be negative (volume=" + volume
                            + ", openInterest=" + openInterest + ")");
        }
    }

    /**
     * Volume divided by open interest, or empty when open interest is zero.
     *
     * Zero open interest is returned as empty rather than as infinity or a
     * large number: a brand-new contract with no open interest has no ratio to
     * speak of, and substituting a sentinel would let it rank above genuine
     * activity.
     */
    public BigDecimal volumeToOpenInterestRatio() {
        if (openInterest == 0) {
            return null;
        }
        return BigDecimal.valueOf(volume)
                .divide(BigDecimal.valueOf(openInterest), 4, RoundingMode.HALF_UP);
    }

    /** Midpoint of the spread, used in preference to last when both sides quote. */
    public BigDecimal mid() {
        if (bid == null || ask == null) {
            return last;
        }
        return bid.add(ask).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }

    /**
     * Ask minus bid. A wide spread is the main reason a gap on an option's own
     * chart may be an artifact rather than a real imbalance, so rules that look
     * at option candles should take this into account.
     */
    public BigDecimal spread() {
        if (bid == null || ask == null) {
            return null;
        }
        return ask.subtract(bid);
    }

    /** A stable identifier for the contract, in OCC-like order. */
    public String contractKey() {
        return underlying + ":" + expiration + ":" + optionType + ":" + strike.stripTrailingZeros().toPlainString();
    }
}
