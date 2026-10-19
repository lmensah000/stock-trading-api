package com.moneyteam.bot.journal.analysis;

import com.moneyteam.bot.journal.ContextFidelity;
import com.moneyteam.bot.journal.ContextSnapshot;
import com.moneyteam.bot.journal.EntrySource;
import com.moneyteam.bot.journal.JournalEntry;
import com.moneyteam.bot.journal.SnapshotCodec;
import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.trading.model.OptionType;
import com.moneyteam.marketdata.model.OptionQuote;
import com.moneyteam.trading.model.enums.OrderSide;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Auditing stated rules against recorded fills.
 *
 * The behaviour worth protecting is that a rule which cannot be evaluated is
 * reported as uncheckable, never as satisfied. Counting absence of evidence as
 * compliance would make a rule look upheld exactly where nothing was known -
 * and that reads as reassurance.
 */
class RuleBookTest {

    private static final Instant NOW = Instant.parse("2026-01-05T14:30:00Z");
    private final SnapshotCodec codec = new SnapshotCodec();

    private static Bar bar(String time, String close) {
        return new Bar("AAPL", Instant.parse(time), Timeframe.D1,
                new BigDecimal(close), new BigDecimal(close), new BigDecimal(close),
                new BigDecimal(close), 1000L);
    }

    private static ContextSnapshot snapshot(String close, String previousClose, OptionQuote quote) {
        return new ContextSnapshot("AAPL", NOW,
                Map.of(Timeframe.D1, List.of(bar("2026-01-05T05:00:00Z", close))),
                previousClose == null ? null : new BigDecimal(previousClose),
                quote, List.of(), new BigDecimal("100000"));
    }

    private static OptionQuote quote(long volume, long openInterest) {
        return new OptionQuote("AAPL", OptionType.CALL, new BigDecimal("105"),
                LocalDate.of(2026, 1, 16), new BigDecimal("2.10"), new BigDecimal("2.20"),
                new BigDecimal("2.15"), volume, openInterest, NOW, NOW.minusSeconds(86400));
    }

    private JournalEntry entry(ContextSnapshot snapshot, ContextFidelity fidelity) {
        return new JournalEntry(7L, "AAPL", OrderSide.BUY, BigDecimal.ONE,
                new BigDecimal("2.15"), NOW, EntrySource.LIVE_CAPTURE, fidelity, NOW)
                .withSnapshot(codec.write(snapshot), NOW);
    }

    @Test
    @DisplayName("a rule upheld by every fill reports no violations")
    void ruleHolds() {
        var rules = List.of(RuleBook.StatedRule.of(
                "I only buy above the previous close", "close > previousClose"));

        var report = new RuleBook(codec).check(rules, List.of(
                entry(snapshot("105.00", "100.00", null), ContextFidelity.UNDERLYING_ONLY),
                entry(snapshot("103.00", "100.00", null), ContextFidelity.UNDERLYING_ONLY)));

        var outcome = report.outcomes().get(0);
        assertThat(outcome.checked()).isEqualTo(2);
        assertThat(outcome.violations()).isZero();
        assertThat(outcome.describe()).contains("held on all 2");
    }

    @Test
    @DisplayName("contradictions are counted and the rate reported")
    void countsContradictions() {
        var rules = List.of(RuleBook.StatedRule.of(
                "I never buy below the previous close", "close > previousClose"));

        // Three fills, two of them below the previous close.
        var report = new RuleBook(codec).check(rules, List.of(
                entry(snapshot("105.00", "100.00", null), ContextFidelity.UNDERLYING_ONLY),
                entry(snapshot("98.00", "100.00", null), ContextFidelity.UNDERLYING_ONLY),
                entry(snapshot("97.00", "100.00", null), ContextFidelity.UNDERLYING_ONLY)));

        var outcome = report.outcomes().get(0);
        assertThat(outcome.checked()).isEqualTo(3);
        assertThat(outcome.violations()).isEqualTo(2);
        assertThat(outcome.violationRate()).isEqualByComparingTo("0.6667");

        // The message states both possibilities, because the report cannot
        // tell a broken rule from a misstated one.
        assertThat(outcome.describe())
                .contains("66.7%")
                .contains("not the rule actually being traded");
    }

    @Test
    @DisplayName("a rule whose feature is missing is uncheckable, never satisfied")
    void missingFeatureIsUncheckable() {
        var rules = List.of(RuleBook.StatedRule.of(
                "I only buy above the previous close", "close > previousClose"));

        // No previous close in the snapshot.
        var report = new RuleBook(codec).check(rules, List.of(
                entry(snapshot("105.00", null, null), ContextFidelity.UNDERLYING_ONLY)));

        var outcome = report.outcomes().get(0);
        assertThat(outcome.checked()).isZero();
        assertThat(outcome.violations()).isZero();
        assertThat(outcome.uncheckable()).isEqualTo(1);
        // Not "held on all 0 fills", which would read as compliance.
        assertThat(outcome.describe()).contains("could not be checked");
    }

    @Test
    @DisplayName("chain features are withheld from fills whose fidelity has no chain")
    void chainFeaturesGatedByFidelity() {
        var rules = List.of(RuleBook.StatedRule.of(
                "I need volume above open interest", "volumeToOpenInterest > 1"));

        // The snapshot happens to carry a quote, but the entry is an import -
        // so that chain data is from a different moment and must not be used.
        var report = new RuleBook(codec).check(rules, List.of(
                entry(snapshot("105.00", "100.00", quote(4000, 500)),
                        ContextFidelity.UNDERLYING_ONLY)));

        assertThat(report.outcomes().get(0).checked()).isZero();
        assertThat(report.outcomes().get(0).uncheckable()).isEqualTo(1);
    }

    @Test
    @DisplayName("chain features are used when the fidelity does carry a chain")
    void chainFeaturesUsedAtFullFidelity() {
        var rules = List.of(RuleBook.StatedRule.of(
                "I need volume above open interest", "volumeToOpenInterest > 1"));

        // 4000 / 500 = 8, so the rule holds.
        var report = new RuleBook(codec).check(rules, List.of(
                entry(snapshot("105.00", "100.00", quote(4000, 500)),
                        ContextFidelity.FULL_CHAIN_AT_FILL)));

        assertThat(report.outcomes().get(0).checked()).isEqualTo(1);
        assertThat(report.outcomes().get(0).violations()).isZero();
    }

    @Test
    @DisplayName("a numeric comparand works as well as a feature one")
    void numericComparand() {
        var rules = List.of(RuleBook.StatedRule.of(
                "I need at least a 4% move", "changePercent >= 4"));

        // 100 -> 105 is 5%; 100 -> 101 is 1%.
        var report = new RuleBook(codec).check(rules, List.of(
                entry(snapshot("105.00", "100.00", null), ContextFidelity.UNDERLYING_ONLY),
                entry(snapshot("101.00", "100.00", null), ContextFidelity.UNDERLYING_ONLY)));

        assertThat(report.outcomes().get(0).violations()).isEqualTo(1);
    }

    @Test
    @DisplayName("an unknown feature is refused at parse time, with the vocabulary listed")
    void unknownFeatureIsRefused() {
        // ema21 is exactly the kind of rule someone would state, and there is
        // no indicator library, so it cannot be checked. Refused loudly rather
        // than accepted and silently never evaluated.
        assertThatThrownBy(() -> RuleBook.StatedRule.of("Above the 21 EMA", "close > ema21"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("neither a number nor a known feature");

        assertThatThrownBy(() -> RuleBook.StatedRule.of("Nonsense", "ema21 > close"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown feature");
    }

    @Test
    @DisplayName("malformed expressions are refused with an example")
    void malformedExpressionIsRefused() {
        assertThatThrownBy(() -> RuleExpression.parse("close"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected one comparison");

        assertThatThrownBy(() -> RuleExpression.parse(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName(">= is not parsed as > followed by garbage")
    void longestOperatorWins() {
        RuleExpression parsed = RuleExpression.parse("changePercent >= 4");

        // Trying ">" first would leave "= 4" as the right-hand side.
        assertThat(parsed.operator()).isEqualTo(RuleExpression.Operator.GTE);
        assertThat(parsed.comparandValue()).isEqualByComparingTo("4");
    }

    @Test
    @DisplayName("every operator behaves")
    void operatorsBehave() {
        Map<String, BigDecimal> features = Map.of("close", new BigDecimal("100"));

        assertThat(RuleExpression.parse("close > 99").evaluate(features)).contains(true);
        assertThat(RuleExpression.parse("close > 100").evaluate(features)).contains(false);
        assertThat(RuleExpression.parse("close >= 100").evaluate(features)).contains(true);
        assertThat(RuleExpression.parse("close < 101").evaluate(features)).contains(true);
        assertThat(RuleExpression.parse("close <= 100").evaluate(features)).contains(true);
        assertThat(RuleExpression.parse("close == 100").evaluate(features)).contains(true);
        assertThat(RuleExpression.parse("close != 100").evaluate(features)).contains(false);
    }

    @Test
    @DisplayName("the summary points at the rules where stated and traded differ")
    void summaryPointsAtTheDifference() {
        var rules = List.of(
                RuleBook.StatedRule.of("Above previous close", "close > previousClose"),
                RuleBook.StatedRule.of("At least 4%", "changePercent >= 4"));

        var report = new RuleBook(codec).check(rules, List.of(
                entry(snapshot("98.00", "100.00", null), ContextFidelity.UNDERLYING_ONLY)));

        assertThat(report.contradicted()).hasSize(2);
        assertThat(report.summary()).contains("stated rule and a traded rule differ");
    }

    @Test
    @DisplayName("no stated rules is reported, not treated as everything passing")
    void noRulesIsReported() {
        var report = new RuleBook(codec).check(List.of(), List.of());

        assertThat(report.summary()).isEqualTo("No rules stated.");
        assertThat(report.outcomes()).isEmpty();
    }
}
