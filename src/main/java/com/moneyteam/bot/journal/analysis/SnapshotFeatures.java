package com.moneyteam.bot.journal.analysis;

import com.moneyteam.bot.journal.ContextSnapshot;
import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.OptionQuote;
import com.moneyteam.marketdata.model.Timeframe;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The measurable facts a snapshot can yield today.
 *
 * <h2>Why the list is short, and what is missing</h2>
 * Every feature here is computable from a snapshot with no indicator library,
 * because there is no indicator library: {@code CLAUDE.md} records that nothing
 * exists beyond the FVG detector. So a stated rule about the 21 EMA, ATR or
 * relative strength <strong>cannot be checked yet</strong> - not because the
 * rule book refuses it, but because the number does not exist anywhere in this
 * codebase. Those become checkable when Phase 6 lands.
 *
 * That is worth knowing before writing rules: the vocabulary below is what can
 * be measured, and a rule outside it will be reported as uncheckable rather
 * than silently treated as satisfied. A rule book that quietly passed rules it
 * could not evaluate would be worse than none, because it would read as
 * compliance.
 */
public final class SnapshotFeatures {

    /** Features over the underlying, available at every fidelity. */
    public static final String CLOSE = "close";
    public static final String OPEN = "open";
    public static final String HIGH = "high";
    public static final String LOW = "low";
    public static final String VOLUME = "volume";
    public static final String PREVIOUS_CLOSE = "previousClose";
    public static final String CHANGE_PERCENT = "changePercent";
    public static final String RANGE_PERCENT = "rangePercent";

    /** Features over the option contract. Null unless the fidelity carries a chain. */
    public static final String CONTRACT_VOLUME = "contractVolume";
    public static final String OPEN_INTEREST = "openInterest";
    public static final String VOLUME_TO_OPEN_INTEREST = "volumeToOpenInterest";

    private SnapshotFeatures() {
    }

    /**
     * Computes every available feature. A feature that cannot be derived is
     * absent from the map rather than zero - zero is a value a rule would
     * compare against and get a wrong answer from.
     */
    public static Map<String, BigDecimal> of(ContextSnapshot snapshot) {
        Map<String, BigDecimal> features = new LinkedHashMap<>();

        Bar latest = snapshot.latestBar(Timeframe.D1);
        if (latest == null) {
            // Fall back to any timeframe present, so an intraday-only snapshot
            // is not treated as featureless.
            latest = snapshot.bars().values().stream()
                    .filter(series -> !series.isEmpty())
                    .map(series -> series.get(series.size() - 1))
                    .findFirst()
                    .orElse(null);
        }

        if (latest != null) {
            features.put(CLOSE, latest.close());
            features.put(OPEN, latest.open());
            features.put(HIGH, latest.high());
            features.put(LOW, latest.low());
            features.put(VOLUME, BigDecimal.valueOf(latest.volume()));

            if (latest.close().signum() > 0) {
                features.put(RANGE_PERCENT, latest.range()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(latest.close(), 4, RoundingMode.HALF_UP));
            }
        }

        BigDecimal previousClose = snapshot.previousClose();
        if (previousClose != null && previousClose.signum() > 0) {
            features.put(PREVIOUS_CLOSE, previousClose);
            if (latest != null) {
                features.put(CHANGE_PERCENT, latest.close().subtract(previousClose)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(previousClose, 4, RoundingMode.HALF_UP));
            }
        }

        contractOf(snapshot).ifPresent(quote -> {
            features.put(CONTRACT_VOLUME, BigDecimal.valueOf(quote.volume()));
            features.put(OPEN_INTEREST, BigDecimal.valueOf(quote.openInterest()));
            // Null on zero open interest rather than a division by zero, and
            // absent from the map rather than zero - a ratio of zero would read
            // as "no interest at all", which is a different claim.
            BigDecimal ratio = quote.volumeToOpenInterestRatio();
            if (ratio != null) {
                features.put(VOLUME_TO_OPEN_INTEREST, ratio);
            }
        });

        return Map.copyOf(features);
    }

    private static Optional<OptionQuote> contractOf(ContextSnapshot snapshot) {
        if (snapshot.contractQuote() != null) {
            return Optional.of(snapshot.contractQuote());
        }
        return snapshot.chain().isEmpty()
                ? Optional.empty()
                : Optional.of(snapshot.chain().get(0));
    }

    /** Every name the rule book understands, for validating a stated rule. */
    public static java.util.Set<String> knownFeatures() {
        return java.util.Set.of(
                CLOSE, OPEN, HIGH, LOW, VOLUME, PREVIOUS_CLOSE, CHANGE_PERCENT,
                RANGE_PERCENT, CONTRACT_VOLUME, OPEN_INTEREST, VOLUME_TO_OPEN_INTEREST);
    }
}
