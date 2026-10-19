package com.moneyteam.bot.journal;

import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.OptionQuote;
import com.moneyteam.trading.model.OptionType;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.strategy.model.Signal;
import com.moneyteam.strategy.rule.EvaluationContext;
import com.moneyteam.strategy.rule.PercentMoveRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The snapshot, and the one property the whole journal rests on.
 *
 * Disagreement mining compares what the trader did against what the rules said.
 * If a snapshot does not rebuild into the context the rules originally saw,
 * every disagreement it reports is an artefact of a lossy reconstruction rather
 * than a fact about the trader - and it would look like a finding, not a bug.
 * So the round trip is asserted on the <em>signal</em>, not just on the fields.
 */
class ContextSnapshotTest {

    private static final Instant CAPTURED = Instant.parse("2026-01-05T14:30:00Z");

    private static Bar bar(String time, String o, String h, String l, String c, long v) {
        return new Bar("AAPL", Instant.parse(time), Timeframe.D1,
                new BigDecimal(o), new BigDecimal(h), new BigDecimal(l), new BigDecimal(c), v);
    }

    private static ContextSnapshot snapshot() {
        return new ContextSnapshot(
                "AAPL",
                CAPTURED,
                Map.of(Timeframe.D1, List.of(
                        bar("2026-01-02T05:00:00Z", "100.00", "101.00", "99.00", "100.00", 1000),
                        bar("2026-01-05T05:00:00Z", "100.00", "106.00", "99.50", "105.00", 5000))),
                new BigDecimal("100.00"),
                new OptionQuote("AAPL", OptionType.CALL, new BigDecimal("105.00"),
                        LocalDate.of(2026, 1, 16), new BigDecimal("2.10"), new BigDecimal("2.20"),
                        new BigDecimal("2.15"), 4000L, 500L, CAPTURED, CAPTURED.minusSeconds(86400)),
                List.of(),
                new BigDecimal("100000"));
    }

    @Test
    @DisplayName("a snapshot survives JSON and replays to the same signal")
    void roundTripsToTheSameSignal() {
        SnapshotCodec codec = new SnapshotCodec();
        ContextSnapshot original = snapshot();

        // The signal the rules would have produced at the time.
        var rule = new PercentMoveRule(new BigDecimal("4"),
                PercentMoveRule.Baseline.PREVIOUS_CLOSE, Timeframe.D1, 0, false);
        Optional<Signal> before = rule.evaluate(original.toEvaluationContext());

        ContextSnapshot restored = codec.read(codec.write(original));
        Optional<Signal> after = rule.evaluate(restored.toEvaluationContext());

        assertThat(before).isPresent();
        assertThat(after).isPresent();
        // Not just "both fired" - the same evidence, or the reconstruction has
        // changed the numbers the decision rested on.
        assertThat(after.get().evidence()).isEqualTo(before.get().evidence());
        assertThat(after.get().dedupeKey()).isEqualTo(before.get().dedupeKey());
    }

    @Test
    @DisplayName("the replayed context carries the historical time, not the replay time")
    void evaluatedAtIsTheCaptureTime() {
        EvaluationContext context = snapshot().toEvaluationContext();

        // Using the clock here is precisely what makes StrategyEngine unusable
        // for replay: a historical signal would be stamped with today.
        assertThat(context.evaluatedAt()).isEqualTo(CAPTURED);
    }

    @Test
    @DisplayName("prices keep their exact decimal value through JSON")
    void pricesSurviveExactly() {
        SnapshotCodec codec = new SnapshotCodec();

        ContextSnapshot restored = codec.read(codec.write(snapshot()));
        Bar latest = restored.latestBar(Timeframe.D1);

        // A price that has been through a double can no longer be compared
        // exactly, and gap detection is an exact inequality.
        assertThat(latest.close()).isEqualByComparingTo("105.00");
        assertThat(restored.previousClose()).isEqualByComparingTo("100.00");
        assertThat(restored.contractQuote().strike()).isEqualByComparingTo("105.00");
    }

    @Test
    @DisplayName("timestamps serialise as readable text, not epoch numbers")
    void timestampsAreReadable() {
        String json = new SnapshotCodec().write(snapshot());

        // A snapshot is read by a person deciding what they saw.
        assertThat(json).contains("2026-01-05T14:30:00Z");
        assertThat(json).doesNotContain("1767623400");
    }

    @Test
    @DisplayName("the option quote's two timestamps both survive")
    void openInterestTimestampSurvives() {
        SnapshotCodec codec = new SnapshotCodec();

        OptionQuote quote = codec.read(codec.write(snapshot())).contractQuote();

        // Open interest publishes once daily, so it is as of a different moment
        // than the quote. Collapsing them would make a day-old OI look current.
        assertThat(quote.asOf()).isEqualTo(CAPTURED);
        assertThat(quote.openInterestAsOf()).isEqualTo(CAPTURED.minusSeconds(86400));
        assertThat(quote.expiration()).isEqualTo(LocalDate.of(2026, 1, 16));
    }

    @Test
    @DisplayName("out-of-order bars are rejected at construction")
    void rejectsOutOfOrderBars() {
        // A snapshot storing reversed bars would invert every indicator on
        // replay, silently.
        assertThatThrownBy(() -> new ContextSnapshot(
                "AAPL", CAPTURED,
                Map.of(Timeframe.D1, List.of(
                        bar("2026-01-05T05:00:00Z", "100", "106", "99", "105", 1),
                        bar("2026-01-02T05:00:00Z", "100", "101", "99", "100", 1))),
                null, null, List.of(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oldest-first");
    }

    @Test
    @DisplayName("a single contract quote is offered to the rules as a one-row chain")
    void singleQuoteBecomesAChain() {
        EvaluationContext context = snapshot().toEvaluationContext();

        // Better than an empty chain, which would make the option rules report
        // nothing and look like they disagreed with the trader.
        assertThat(context.optionChain()).hasSize(1);
        assertThat(context.optionChain().get(0).openInterest()).isEqualTo(500L);
    }

    @Test
    @DisplayName("a full chain takes precedence over the single quote")
    void fullChainWins() {
        ContextSnapshot withChain = new ContextSnapshot(
                "AAPL", CAPTURED,
                Map.of(Timeframe.D1, List.of(bar("2026-01-02T05:00:00Z", "100", "101", "99", "100", 1))),
                null,
                snapshot().contractQuote(),
                List.of(snapshot().contractQuote(), snapshot().contractQuote()),
                null);

        assertThat(withChain.toEvaluationContext().optionChain()).hasSize(2);
    }

    @Test
    @DisplayName("a snapshot with no bars is not usable, and says so")
    void emptySnapshotIsNotUsable() {
        ContextSnapshot empty = new ContextSnapshot(
                "AAPL", CAPTURED, Map.of(), null, null, List.of(), null);

        // Reported rather than replayed: evaluating nothing would produce
        // "no rule fired", which reads identically to a real articulation gap.
        assertThat(empty.isUsable()).isFalse();
        assertThat(snapshot().isUsable()).isTrue();
    }

    @Test
    @DisplayName("an unknown field in stored JSON does not break reading")
    void toleratesUnknownFields() {
        SnapshotCodec codec = new SnapshotCodec();
        String json = codec.write(snapshot())
                .replaceFirst("\\{", "{\"somethingAddedLater\":123,");

        // A write-once archive must stay readable after a field is added, or
        // every old snapshot becomes unreplayable on the next release.
        assertThat(codec.read(json).ticker()).isEqualTo("AAPL");
    }

    @Test
    @DisplayName("fidelity decides whether chain features may be read at all")
    void fidelityGatesChainFeatures() {
        assertThat(ContextFidelity.UNDERLYING_ONLY.hasChainContext()).isFalse();
        assertThat(ContextFidelity.UNDERLYING_PLUS_EOD_CHAIN.hasChainContext()).isTrue();
        assertThat(ContextFidelity.FULL_CHAIN_AT_FILL.hasChainContext()).isTrue();
    }
}
