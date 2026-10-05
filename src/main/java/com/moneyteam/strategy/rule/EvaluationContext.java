package com.moneyteam.strategy.rule;

import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.OptionQuote;
import com.moneyteam.marketdata.model.Timeframe;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Everything a rule needs to evaluate one ticker, captured as one immutable
 * snapshot.
 *
 * <h2>Why a snapshot rather than a provider reference</h2>
 * Rules could each call the market-data provider themselves, but then two rules
 * examining the same ticker could see different prices, and a signal reported
 * as "percentage move confirmed by a gap" might rest on data a minute apart.
 * Building the context once per ticker means every rule reasons over the same
 * instant, and makes each rule a pure function of its input - which is what
 * makes them trivially testable and safe to run in parallel.
 */
public final class EvaluationContext {

    private final String ticker;
    private final Instant evaluatedAt;
    private final Map<Timeframe, List<Bar>> barsByTimeframe;
    private final List<OptionQuote> optionChain;
    private final Map<String, Map<Timeframe, List<Bar>>> optionBarsByContract;
    private final BigDecimal previousClose;
    private final BigDecimal accountEquity;

    private EvaluationContext(Builder builder) {
        this.ticker = Objects.requireNonNull(builder.ticker, "ticker");
        this.evaluatedAt = Objects.requireNonNull(builder.evaluatedAt, "evaluatedAt");
        this.barsByTimeframe = Collections.unmodifiableMap(new EnumMap<>(builder.barsByTimeframe));
        this.optionChain = List.copyOf(builder.optionChain);
        this.optionBarsByContract = Map.copyOf(builder.optionBarsByContract);
        this.previousClose = builder.previousClose;
        this.accountEquity = builder.accountEquity;
    }

    public String ticker() {
        return ticker;
    }

    public Instant evaluatedAt() {
        return evaluatedAt;
    }

    /** Bars for a timeframe, oldest-first; empty when that timeframe was not loaded. */
    public List<Bar> bars(Timeframe timeframe) {
        return barsByTimeframe.getOrDefault(timeframe, List.of());
    }

    /** The most recent bar on a timeframe, or null when none was loaded. */
    public Bar latestBar(Timeframe timeframe) {
        List<Bar> series = bars(timeframe);
        return series.isEmpty() ? null : series.get(series.size() - 1);
    }

    public List<OptionQuote> optionChain() {
        return optionChain;
    }

    /** Bars for one option contract on a timeframe; empty is a normal answer. */
    public List<Bar> optionBars(String contractKey, Timeframe timeframe) {
        return optionBarsByContract.getOrDefault(contractKey, Map.of())
                .getOrDefault(timeframe, List.of());
    }

    /** Previous session's close, or null when unknown. */
    public BigDecimal previousClose() {
        return previousClose;
    }

    /** Account equity, used for risk-based sizing. Null when not supplied. */
    public BigDecimal accountEquity() {
        return accountEquity;
    }

    public static Builder builder(String ticker, Instant evaluatedAt) {
        return new Builder(ticker, evaluatedAt);
    }

    public static final class Builder {
        private final String ticker;
        private final Instant evaluatedAt;
        private final Map<Timeframe, List<Bar>> barsByTimeframe = new EnumMap<>(Timeframe.class);
        private final Map<String, Map<Timeframe, List<Bar>>> optionBarsByContract = new java.util.HashMap<>();
        private List<OptionQuote> optionChain = List.of();
        private BigDecimal previousClose;
        private BigDecimal accountEquity;

        private Builder(String ticker, Instant evaluatedAt) {
            this.ticker = ticker;
            this.evaluatedAt = evaluatedAt;
        }

        public Builder bars(Timeframe timeframe, List<Bar> series) {
            Bar.requireOldestFirst(series);
            barsByTimeframe.put(timeframe, List.copyOf(series));
            return this;
        }

        public Builder optionChain(List<OptionQuote> chain) {
            this.optionChain = List.copyOf(chain);
            return this;
        }

        public Builder optionBars(String contractKey, Timeframe timeframe, List<Bar> series) {
            Bar.requireOldestFirst(series);
            optionBarsByContract
                    .computeIfAbsent(contractKey, k -> new EnumMap<>(Timeframe.class))
                    .put(timeframe, List.copyOf(series));
            return this;
        }

        public Builder previousClose(BigDecimal close) {
            this.previousClose = close;
            return this;
        }

        public Builder accountEquity(BigDecimal equity) {
            this.accountEquity = equity;
            return this;
        }

        public EvaluationContext build() {
            return new EvaluationContext(this);
        }
    }
}
