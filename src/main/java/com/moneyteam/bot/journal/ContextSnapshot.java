package com.moneyteam.bot.journal;

import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.OptionQuote;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.strategy.rule.EvaluationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The market as it stood when a fill happened.
 *
 * The one property that matters: this must rebuild into an
 * {@link EvaluationContext} that produces the <em>same</em> signals the live
 * rules produced at the time. Disagreement mining compares the trader's
 * behaviour against what the rules said, and if the reconstruction is lossy
 * then every disagreement it reports is an artefact of the reconstruction
 * rather than a fact about the trader. A round-trip test guards it.
 *
 * Serialised to JSON by Jackson and stored on the entry. A record rather than
 * an entity: snapshots are written once, read whole, and never queried by
 * field, so columns would buy nothing and a schema change per new feature
 * would cost a migration.
 *
 * @param ticker       underlying
 * @param capturedAt   the instant this represents - becomes the context's
 *                     {@code evaluatedAt}, so replayed signals carry the
 *                     historical time rather than the time of the replay
 * @param bars         bars per timeframe, oldest-first
 * @param previousClose prior session's close, or null
 * @param contractQuote the traded contract's own quote, when the fidelity
 *                     allows; null otherwise
 * @param chain        the chain at that moment, when available
 * @param accountEquity equity at the time, for risk-based sizing
 */
public record ContextSnapshot(
        String ticker,
        Instant capturedAt,
        Map<Timeframe, List<Bar>> bars,
        BigDecimal previousClose,
        OptionQuote contractQuote,
        List<OptionQuote> chain,
        BigDecimal accountEquity) {

    public ContextSnapshot {
        // Defensive copies and the ordering guard: a snapshot that stored
        // reversed bars would invert every indicator on replay, and silently.
        Map<Timeframe, List<Bar>> copied = new EnumMap<>(Timeframe.class);
        if (bars != null) {
            bars.forEach((timeframe, series) -> {
                Bar.requireOldestFirst(series);
                copied.put(timeframe, List.copyOf(series));
            });
        }
        bars = Map.copyOf(copied);
        chain = chain == null ? List.of() : List.copyOf(chain);
    }

    /**
     * Rebuilds the context the rules would have seen.
     *
     * {@code capturedAt} is used for {@code evaluatedAt} rather than now, so a
     * replayed signal carries the time it would have had. Using the clock here
     * is the same mistake that makes {@code StrategyEngine} unsuitable for
     * replay.
     */
    public EvaluationContext toEvaluationContext() {
        EvaluationContext.Builder builder = EvaluationContext.builder(ticker, capturedAt)
                .previousClose(previousClose)
                .accountEquity(accountEquity);

        bars.forEach(builder::bars);

        if (!chain.isEmpty()) {
            builder.optionChain(chain);
        } else if (contractQuote != null) {
            // A single-contract snapshot still gives the option rules something
            // to read, which is better than presenting an empty chain and
            // having them silently report nothing.
            builder.optionChain(List.of(contractQuote));
        }

        return builder.build();
    }

    /** The most recent bar on a timeframe, or null when that series is absent. */
    public Bar latestBar(Timeframe timeframe) {
        List<Bar> series = bars.getOrDefault(timeframe, List.of());
        return series.isEmpty() ? null : series.get(series.size() - 1);
    }

    /** Whether this snapshot carries enough to evaluate anything at all. */
    public boolean isUsable() {
        return ticker != null && capturedAt != null
                && bars.values().stream().anyMatch(series -> !series.isEmpty());
    }
}
