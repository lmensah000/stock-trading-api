package com.moneyteam.strategy.rule;

import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.strategy.model.Direction;
import com.moneyteam.strategy.model.Signal;
import com.moneyteam.strategy.model.SignalType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PercentMoveRuleTest {

    private static final Instant NOW = Instant.parse("2024-03-04T19:00:00Z");

    private static Bar bar(int index, String open, String close) {
        BigDecimal o = new BigDecimal(open);
        BigDecimal c = new BigDecimal(close);
        BigDecimal high = o.max(c);
        BigDecimal low = o.min(c);
        return new Bar("AAPL", NOW.minus(60L - index * 5L, ChronoUnit.MINUTES),
                Timeframe.M5, o, high, low, c, 10_000L);
    }

    private static EvaluationContext context(List<Bar> bars, String previousClose) {
        EvaluationContext.Builder b = EvaluationContext.builder("AAPL", NOW).bars(Timeframe.M5, bars);
        if (previousClose != null) {
            b.previousClose(new BigDecimal(previousClose));
        }
        return b.build();
    }

    @Test
    @DisplayName("fires when the move from the previous close clears the threshold")
    void firesOnPreviousClose() {
        // 100 -> 105 is +5%, above a 4% threshold
        var rule = PercentMoveRule.upFromPreviousClose(new BigDecimal("4"));

        Optional<Signal> signal = rule.evaluate(context(List.of(bar(0, "101", "105")), "100"));

        assertThat(signal).isPresent();
        assertThat(signal.get().type()).isEqualTo(SignalType.PERCENT_MOVE);
        assertThat(signal.get().direction()).isEqualTo(Direction.BULLISH);
        assertThat(signal.get().evidence()).containsEntry("changePercent", "5.0000");
    }

    @Test
    @DisplayName("a move exactly at the threshold counts as meeting it")
    void thresholdIsInclusive() {
        // Exactly +4.0000% against a 4% threshold.
        var rule = PercentMoveRule.upFromPreviousClose(new BigDecimal("4"));

        assertThat(rule.evaluate(context(List.of(bar(0, "101", "104")), "100"))).isPresent();
    }

    @Test
    @DisplayName("a move just under the threshold does not fire")
    void justBelowThresholdDoesNotFire() {
        var rule = PercentMoveRule.upFromPreviousClose(new BigDecimal("4"));

        assertThat(rule.evaluate(context(List.of(bar(0, "101", "103.99")), "100"))).isEmpty();
    }

    @Test
    @DisplayName("downward moves are ignored unless explicitly enabled")
    void downMovesIgnoredByDefault() {
        var upOnly = PercentMoveRule.upFromPreviousClose(new BigDecimal("4"));

        assertThat(upOnly.evaluate(context(List.of(bar(0, "99", "94")), "100"))).isEmpty();
    }

    @Test
    @DisplayName("downward moves fire as bearish when enabled")
    void downMovesFireWhenEnabled() {
        var bothWays = new PercentMoveRule(
                new BigDecimal("4"), PercentMoveRule.Baseline.PREVIOUS_CLOSE, Timeframe.M5, 0, true);

        Optional<Signal> signal = bothWays.evaluate(context(List.of(bar(0, "99", "94")), "100"));

        assertThat(signal).isPresent();
        assertThat(signal.get().direction()).isEqualTo(Direction.BEARISH);
        assertThat(signal.get().evidence()).containsEntry("changePercent", "-6.0000");
    }

    @Test
    @DisplayName("SESSION_OPEN measures from the first loaded bar's open")
    void sessionOpenBaseline() {
        var rule = new PercentMoveRule(
                new BigDecimal("3"), PercentMoveRule.Baseline.SESSION_OPEN, Timeframe.M5, 0, false);

        // opens at 100, latest closes at 104 -> +4%
        List<Bar> bars = List.of(bar(0, "100", "101"), bar(1, "101", "104"));

        Optional<Signal> signal = rule.evaluate(context(bars, null));

        assertThat(signal).isPresent();
        assertThat(signal.get().evidence()).containsEntry("baselinePrice", "100");
    }

    @Test
    @DisplayName("N_BARS_AGO measures recent momentum, not the whole session")
    void nBarsAgoBaseline() {
        var rule = new PercentMoveRule(
                new BigDecimal("2"), PercentMoveRule.Baseline.N_BARS_AGO, Timeframe.M5, 2, false);

        // Two bars back closed at 100; latest closes at 103 -> +3%.
        // From the session open of 90 it would be +14%, so this proves the
        // baseline actually shifts rather than always reading the first bar.
        List<Bar> bars = List.of(
                bar(0, "90", "95"),
                bar(1, "95", "100"),
                bar(2, "100", "101"),
                bar(3, "101", "103"));

        Optional<Signal> signal = rule.evaluate(context(bars, null));

        assertThat(signal).isPresent();
        assertThat(signal.get().evidence()).containsEntry("baselinePrice", "100");
        assertThat(signal.get().evidence()).containsEntry("changePercent", "3.0000");
    }

    @Test
    @DisplayName("no baseline means no signal, rather than a fabricated percentage")
    void missingBaselineYieldsNothing() {
        var rule = PercentMoveRule.upFromPreviousClose(new BigDecimal("4"));

        assertThat(rule.evaluate(context(List.of(bar(0, "101", "105")), null))).isEmpty();
    }

    @Test
    @DisplayName("no bars means no signal")
    void noBarsYieldsNothing() {
        var rule = PercentMoveRule.upFromPreviousClose(new BigDecimal("4"));

        assertThat(rule.evaluate(context(List.of(), "100"))).isEmpty();
    }

    @Test
    @DisplayName("the dedupe key describes the condition, so repeat scans collapse")
    void dedupeKeyIsStableAcrossObservations() {
        var rule = PercentMoveRule.upFromPreviousClose(new BigDecimal("4"));

        String first = rule.evaluate(context(List.of(bar(0, "101", "105")), "100")).orElseThrow().dedupeKey();
        String second = rule.evaluate(context(List.of(bar(0, "101", "107")), "100")).orElseThrow().dedupeKey();

        assertThat(first).isEqualTo(second);
    }
}
