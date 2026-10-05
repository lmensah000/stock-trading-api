package com.moneyteam.strategy.rule;

import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.OptionQuote;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.strategy.indicator.FairValueGapDetector;
import com.moneyteam.strategy.model.Confidence;
import com.moneyteam.strategy.model.Direction;
import com.moneyteam.strategy.model.FairValueGap;
import com.moneyteam.strategy.model.GapSource;
import com.moneyteam.strategy.model.Signal;
import com.moneyteam.strategy.model.SignalType;
import com.moneyteam.trading.model.OptionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * The composed "high-value option trade" condition: unusual volume relative to
 * open interest, corroborated by Fair Value Gaps pointing the same way across
 * the 5-minute, 15-minute and 1-hour charts.
 *
 * <h2>How confidence is earned</h2>
 * Volume alone is {@link Confidence#MEDIUM} at best - it says positioning
 * changed, not which way it resolves. Confidence rises with corroboration:
 * <ul>
 *   <li>Gaps on the <strong>underlying</strong> across all three timeframes,
 *       agreeing with the option's direction, reach {@link Confidence#HIGH}.</li>
 *   <li>Two agreeing timeframes reach {@link Confidence#MEDIUM}.</li>
 *   <li>Corroboration found <strong>only</strong> on the option contract's own
 *       candles is capped at {@link Confidence#LOW}, however clean it looks.
 *       Thin contracts leave empty intervals and wide spreads make prints hop
 *       between bid and ask, so those gaps are often spread artifacts rather
 *       than imbalance - and an option's price also moves on implied volatility
 *       and decay, so a gap there may be neither buying nor selling pressure.</li>
 * </ul>
 * Both sources are evaluated so the two can be compared in practice, but the
 * distinction stays visible in the signal rather than being averaged away.
 *
 * Immutable and thread-safe.
 */
public final class HighValueOptionRule implements StrategyRule {

    private final VolumeOpenInterestRule volumeRule;
    private final List<Timeframe> confirmationTimeframes;
    private final boolean inspectOptionCharts;

    public HighValueOptionRule(VolumeOpenInterestRule volumeRule,
                               List<Timeframe> confirmationTimeframes,
                               boolean inspectOptionCharts) {
        this.volumeRule = Objects.requireNonNull(volumeRule, "volumeRule");
        this.confirmationTimeframes = List.copyOf(confirmationTimeframes);
        this.inspectOptionCharts = inspectOptionCharts;
    }

    /** Standard volume thresholds, confirmed on 5m/15m/1h, inspecting both chart sources. */
    public static HighValueOptionRule standard() {
        return new HighValueOptionRule(
                VolumeOpenInterestRule.standard(),
                List.of(Timeframe.intraday()),
                true);
    }

    @Override
    public Optional<Signal> evaluate(EvaluationContext context) {
        Optional<OptionQuote> candidate = context.optionChain().stream()
                .filter(volumeRule::qualifies)
                .max(Comparator.comparing(q -> Objects.requireNonNullElse(
                        q.volumeToOpenInterestRatio(), BigDecimal.ZERO)));

        if (candidate.isEmpty()) {
            return Optional.empty();
        }

        OptionQuote quote = candidate.get();
        Direction expected = quote.optionType() == OptionType.PUT ? Direction.BEARISH : Direction.BULLISH;

        List<FairValueGap> underlyingAgreeing =
                agreeingGaps(context, expected, GapSource.UNDERLYING, quote);
        List<FairValueGap> optionAgreeing = inspectOptionCharts
                ? agreeingGaps(context, expected, GapSource.OPTION_CONTRACT, quote)
                : List.of();

        if (underlyingAgreeing.isEmpty() && optionAgreeing.isEmpty()) {
            // Volume without any directional corroboration. The volume rule
            // reports that on its own; this rule stays silent rather than
            // emitting a duplicate at lower value.
            return Optional.empty();
        }

        Confidence confidence = scoreConfidence(underlyingAgreeing, optionAgreeing);
        return Optional.of(buildSignal(context, quote, expected, confidence,
                underlyingAgreeing, optionAgreeing));
    }

    /**
     * Unfilled gaps on the given source that point the same way as the option's
     * implied direction. A bullish gap under a call-volume spike corroborates;
     * a bearish one contradicts and is not counted.
     */
    private List<FairValueGap> agreeingGaps(EvaluationContext context,
                                            Direction expected,
                                            GapSource source,
                                            OptionQuote quote) {
        List<FairValueGap> agreeing = new ArrayList<>();
        for (Timeframe timeframe : confirmationTimeframes) {
            List<Bar> bars = source == GapSource.UNDERLYING
                    ? context.bars(timeframe)
                    : context.optionBars(quote.contractKey(), timeframe);

            FairValueGapDetector.detectUnfilled(bars, source).stream()
                    .filter(gap -> gap.direction() == expected)
                    // Most recent gap per timeframe: an imbalance from days ago
                    // is weaker corroboration than one that just formed.
                    .max(Comparator.comparing(FairValueGap::formedAt))
                    .ifPresent(agreeing::add);
        }
        return agreeing;
    }

    private Confidence scoreConfidence(List<FairValueGap> underlying, List<FairValueGap> option) {
        if (underlying.isEmpty()) {
            // Option-chart evidence only - structurally unreliable, so capped.
            return GapSource.OPTION_CONTRACT.confidenceCeiling();
        }
        if (underlying.size() >= confirmationTimeframes.size()) {
            return Confidence.HIGH;
        }
        if (underlying.size() >= 2) {
            return Confidence.MEDIUM;
        }
        return Confidence.LOW;
    }

    private Signal buildSignal(EvaluationContext context,
                               OptionQuote quote,
                               Direction direction,
                               Confidence confidence,
                               List<FairValueGap> underlying,
                               List<FairValueGap> option) {

        Map<String, String> evidence = new LinkedHashMap<>();
        evidence.put("contract", quote.contractKey());
        evidence.put("volume", Long.toString(quote.volume()));
        evidence.put("openInterest", Long.toString(quote.openInterest()));
        evidence.put("volumeToOpenInterestRatio", quote.volumeToOpenInterestRatio().toPlainString());
        evidence.put("openInterestAsOf", quote.openInterestAsOf().toString());
        evidence.put("underlyingGapTimeframes", describe(underlying));
        evidence.put("optionChartGapTimeframes", describe(option));
        evidence.put("confidenceBasis", underlying.isEmpty()
                ? "option-contract charts only; capped at LOW because thin contracts and wide spreads make those gaps unreliable"
                : underlying.size() + " of " + confirmationTimeframes.size() + " underlying timeframes agree");
        if (quote.spread() != null) {
            evidence.put("spread", quote.spread().toPlainString());
        }

        String summary = "%s %s %s: volume %d vs open interest %d, %s".formatted(
                context.ticker(),
                quote.expiration(),
                quote.optionType(),
                quote.volume(),
                quote.openInterest(),
                underlying.isEmpty()
                        ? "corroborated only on the option's own chart"
                        : underlying.size() + " underlying timeframe(s) showing an agreeing gap");

        return new Signal(
                SignalType.HIGH_VALUE_OPTION,
                context.ticker(),
                direction,
                confidence,
                context.evaluatedAt(),
                summary,
                "HIGH_VALUE_OPTION:" + quote.contractKey(),
                evidence);
    }

    private static String describe(List<FairValueGap> gaps) {
        if (gaps.isEmpty()) {
            return "none";
        }
        StringJoiner joiner = new StringJoiner(",");
        gaps.forEach(g -> joiner.add(g.timeframe().name()));
        return joiner.toString();
    }

    @Override
    public String name() {
        return "HighValueOptionRule";
    }
}
