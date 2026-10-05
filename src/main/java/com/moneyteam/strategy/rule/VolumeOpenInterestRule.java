package com.moneyteam.strategy.rule;

import com.moneyteam.marketdata.model.OptionQuote;
import com.moneyteam.strategy.model.Confidence;
import com.moneyteam.strategy.model.Direction;
import com.moneyteam.strategy.model.Signal;
import com.moneyteam.strategy.model.SignalType;
import com.moneyteam.trading.model.OptionType;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Fires when an option contract trades far more volume than its open interest.
 *
 * A ratio above 1 means more contracts changed hands today than existed when
 * the session opened, which implies new positioning rather than existing
 * positions being shuffled.
 *
 * <h2>The volume floor is the whole point</h2>
 * Ratio alone is close to meaningless on illiquid contracts: ten contracts
 * against two open interest is a ratio of five and tells you nothing. Requiring
 * a minimum absolute volume alongside the ratio is what separates a signal from
 * an artifact of a near-empty contract. A rule with only the ratio would fire
 * constantly on the least significant contracts in the chain.
 *
 * <h2>The denominator is stale, by construction</h2>
 * Open interest is published once daily, so an intraday ratio compares live
 * volume to the previous session's open interest. That is the conventional
 * measure and it is sound, but the staleness is recorded in the signal's
 * evidence rather than left implicit, because a ratio computed against a
 * two-day-old denominator is a weaker claim than one against yesterday's.
 *
 * Immutable and thread-safe.
 */
public final class VolumeOpenInterestRule implements StrategyRule {

    private final BigDecimal ratioThreshold;
    private final long minimumVolume;
    private final Duration maximumOpenInterestAge;

    /**
     * @param ratioThreshold         minimum volume/open-interest, e.g. 2.0
     * @param minimumVolume          absolute contract floor below which nothing fires
     * @param maximumOpenInterestAge beyond this the denominator is too old to trust
     */
    public VolumeOpenInterestRule(BigDecimal ratioThreshold,
                                  long minimumVolume,
                                  Duration maximumOpenInterestAge) {
        this.ratioThreshold = Objects.requireNonNull(ratioThreshold, "ratioThreshold");
        this.minimumVolume = minimumVolume;
        this.maximumOpenInterestAge = Objects.requireNonNull(maximumOpenInterestAge, "maximumOpenInterestAge");

        if (ratioThreshold.signum() <= 0) {
            throw new IllegalArgumentException("ratioThreshold must be positive: " + ratioThreshold);
        }
        if (minimumVolume < 0) {
            throw new IllegalArgumentException("minimumVolume cannot be negative: " + minimumVolume);
        }
    }

    /** Ratio of 2.0 on at least 500 contracts, with open interest no older than two days. */
    public static VolumeOpenInterestRule standard() {
        return new VolumeOpenInterestRule(BigDecimal.valueOf(2), 500, Duration.ofDays(2));
    }

    @Override
    public Optional<Signal> evaluate(EvaluationContext context) {
        return context.optionChain().stream()
                .filter(this::qualifies)
                .max(Comparator.comparing(q -> Objects.requireNonNullElse(
                        q.volumeToOpenInterestRatio(), BigDecimal.ZERO)))
                .map(best -> toSignal(context, best));
    }

    /** Exposed so the composed rule can reuse the same qualification logic. */
    public boolean qualifies(OptionQuote quote) {
        if (quote.volume() < minimumVolume) {
            return false;
        }
        BigDecimal ratio = quote.volumeToOpenInterestRatio();
        if (ratio == null) {
            // Zero open interest: no ratio exists. A brand-new contract is not
            // evidence of unusual rotation, so it does not qualify here.
            return false;
        }
        if (ratio.compareTo(ratioThreshold) < 0) {
            return false;
        }
        return !isOpenInterestTooOld(quote);
    }

    private boolean isOpenInterestTooOld(OptionQuote quote) {
        Duration age = Duration.between(quote.openInterestAsOf(), quote.asOf());
        return age.compareTo(maximumOpenInterestAge) > 0;
    }

    private Signal toSignal(EvaluationContext context, OptionQuote quote) {
        BigDecimal ratio = quote.volumeToOpenInterestRatio();
        Duration oiAge = Duration.between(quote.openInterestAsOf(), quote.asOf());

        Map<String, String> evidence = new LinkedHashMap<>();
        evidence.put("contract", quote.contractKey());
        evidence.put("optionType", quote.optionType().name());
        evidence.put("strike", quote.strike().toPlainString());
        evidence.put("expiration", quote.expiration().toString());
        evidence.put("volume", Long.toString(quote.volume()));
        evidence.put("openInterest", Long.toString(quote.openInterest()));
        evidence.put("volumeToOpenInterestRatio", ratio.toPlainString());
        evidence.put("ratioThreshold", ratioThreshold.toPlainString());
        evidence.put("openInterestAsOf", quote.openInterestAsOf().toString());
        evidence.put("openInterestAgeHours", Long.toString(oiAge.toHours()));
        if (quote.spread() != null) {
            evidence.put("spread", quote.spread().toPlainString());
        }

        String summary = "%s %s %s has volume %d against open interest %d (ratio %s)".formatted(
                context.ticker(),
                quote.expiration(),
                quote.optionType(),
                quote.volume(),
                quote.openInterest(),
                ratio.toPlainString());

        return new Signal(
                SignalType.UNUSUAL_OPTION_VOLUME,
                context.ticker(),
                directionOf(quote.optionType()),
                // Unusual volume says positioning changed; it does not say which
                // way it will resolve. Without corroboration this is medium at best.
                Confidence.MEDIUM,
                context.evaluatedAt(),
                summary,
                "UNUSUAL_OPTION_VOLUME:" + quote.contractKey(),
                evidence);
    }

    /**
     * Call buying leans bullish and put buying bearish, but only loosely: the
     * chain shows no counterparty, so heavy call volume could equally be
     * covered-call selling. Direction here is a hint, not a conclusion.
     */
    private static Direction directionOf(OptionType type) {
        return type == OptionType.PUT ? Direction.BEARISH : Direction.BULLISH;
    }

    @Override
    public String name() {
        return "VolumeOpenInterestRule(ratio>=%s, volume>=%d)"
                .formatted(ratioThreshold.toPlainString(), minimumVolume);
    }
}
