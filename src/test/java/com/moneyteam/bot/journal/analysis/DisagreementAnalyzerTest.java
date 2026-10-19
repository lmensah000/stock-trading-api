package com.moneyteam.bot.journal.analysis;

import com.moneyteam.bot.journal.ContextFidelity;
import com.moneyteam.bot.journal.ContextSnapshot;
import com.moneyteam.bot.journal.EntrySource;
import com.moneyteam.bot.journal.JournalEntry;
import com.moneyteam.bot.journal.Judgement;
import com.moneyteam.bot.journal.SnapshotCodec;
import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.strategy.rule.PercentMoveRule;
import com.moneyteam.strategy.rule.StrategyRule;
import com.moneyteam.trading.model.enums.OrderSide;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comparing recorded behaviour against the encoded rules.
 *
 * The bucket that matters is {@code YOU_TOOK_RULES_SILENT}: trades taken while
 * every rule stayed quiet. Each one is a decision made for a reason the system
 * does not contain, which is exactly the knowledge that was never written
 * down - recovered by comparison rather than by asking for a description.
 */
class DisagreementAnalyzerTest {

    private static final long USER = 7L;
    private static final Instant NOW = Instant.parse("2026-01-05T14:30:00Z");

    private final SnapshotCodec codec = new SnapshotCodec();

    /** 4% up from the previous close, on daily bars. */
    private static StrategyRule rule() {
        return new PercentMoveRule(new BigDecimal("4"),
                PercentMoveRule.Baseline.PREVIOUS_CLOSE, Timeframe.D1, 0, false);
    }

    private static Bar bar(String time, String close) {
        return new Bar("AAPL", Instant.parse(time), Timeframe.D1,
                new BigDecimal(close), new BigDecimal(close), new BigDecimal(close),
                new BigDecimal(close), 1000L);
    }

    /** A snapshot whose move either clears the 4% threshold or does not. */
    private static ContextSnapshot snapshotMoving(String closeToday, String previousClose) {
        return new ContextSnapshot(
                "AAPL", NOW,
                Map.of(Timeframe.D1, List.of(
                        bar("2026-01-02T05:00:00Z", previousClose),
                        bar("2026-01-05T05:00:00Z", closeToday))),
                new BigDecimal(previousClose), null, List.of(), new BigDecimal("100000"));
    }

    private JournalEntry entry(ContextSnapshot snapshot, ContextFidelity fidelity) {
        JournalEntry entry = new JournalEntry(
                USER, "AAPL", OrderSide.BUY, new BigDecimal("1"), new BigDecimal("2.15"),
                NOW, EntrySource.LIVE_CAPTURE, fidelity, NOW);
        return snapshot == null ? entry : entry.withSnapshot(codec.write(snapshot), NOW);
    }

    private DisagreementAnalyzer analyzer() {
        return new DisagreementAnalyzer(List.of(rule()), codec);
    }

    @Test
    @DisplayName("a trade the rules also flagged reads as AGREED")
    void agreedWhenRuleFires() {
        // 100 -> 105 is a 5% move, so the rule fires.
        JournalEntry taken = entry(snapshotMoving("105.00", "100.00"),
                ContextFidelity.FULL_CHAIN_AT_FILL);

        Disagreement result = analyzer().compare(taken);

        assertThat(result.verdict()).isEqualTo(Disagreement.Verdict.AGREED);
        assertThat(result.firedRules()).hasSize(1);
    }

    @Test
    @DisplayName("a trade taken while every rule stayed silent is the articulation gap")
    void silentRulesIsTheGap() {
        // 100 -> 101 is a 1% move. The rule says nothing, yet the trade was
        // taken - so something on the chart justified it that no rule encodes.
        JournalEntry taken = entry(snapshotMoving("101.00", "100.00"),
                ContextFidelity.FULL_CHAIN_AT_FILL);

        Disagreement result = analyzer().compare(taken);

        assertThat(result.verdict()).isEqualTo(Disagreement.Verdict.YOU_TOOK_RULES_SILENT);
        assertThat(result.firedRules()).isEmpty();
    }

    @Test
    @DisplayName("a setup the rules flagged and nobody traded reads as passed over")
    void flaggedButPassedOver() {
        Disagreement result = analyzer().comparePassedSetup(
                "AAPL", snapshotMoving("105.00", "100.00"), ContextFidelity.FULL_CHAIN_AT_FILL);

        assertThat(result.verdict()).isEqualTo(Disagreement.Verdict.RULES_FIRED_YOU_PASSED);
        // No fill, so no journal entry id.
        assertThat(result.journalEntryId()).isNull();
    }

    @Test
    @DisplayName("a missing snapshot is reported, not silently dropped")
    void missingSnapshotIsReported() {
        Disagreement result = analyzer().compare(entry(null, ContextFidelity.UNDERLYING_ONLY));

        // A skipped entry would make the other counts look like they covered
        // the whole journal.
        assertThat(result.verdict()).isEqualTo(Disagreement.Verdict.NOT_EVALUABLE);
    }

    @Test
    @DisplayName("unreadable snapshot JSON is reported rather than throwing")
    void corruptSnapshotIsReported() {
        JournalEntry broken = new JournalEntry(
                USER, "AAPL", OrderSide.BUY, BigDecimal.ONE, new BigDecimal("2.15"),
                NOW, EntrySource.BROKER_IMPORT, ContextFidelity.UNDERLYING_ONLY, NOW)
                .withSnapshot("{not valid json", NOW);

        assertThat(analyzer().compare(broken).verdict())
                .isEqualTo(Disagreement.Verdict.NOT_EVALUABLE);
    }

    @Test
    @DisplayName("the report counts each bucket and names the unexplained trades")
    void reportCountsBuckets() {
        List<JournalEntry> entries = List.of(
                entry(snapshotMoving("105.00", "100.00"), ContextFidelity.FULL_CHAIN_AT_FILL),
                entry(snapshotMoving("101.00", "100.00"), ContextFidelity.FULL_CHAIN_AT_FILL),
                entry(snapshotMoving("101.50", "100.00"), ContextFidelity.FULL_CHAIN_AT_FILL),
                entry(null, ContextFidelity.FULL_CHAIN_AT_FILL));

        DisagreementReport report =
                analyzer().analyse(entries, ContextFidelity.FULL_CHAIN_AT_FILL);

        assertThat(report.countOf(Disagreement.Verdict.AGREED)).isEqualTo(1);
        assertThat(report.rulesSilentCount()).isEqualTo(2);
        assertThat(report.notEvaluableCount()).isEqualTo(1);
        assertThat(report.unexplainedTrades()).hasSize(2);
    }

    @Test
    @DisplayName("the summary names the silent trades as the rules not yet written down")
    void summarySaysWhatTheGapIs() {
        String summary = analyzer()
                .analyse(List.of(entry(snapshotMoving("101.00", "100.00"),
                        ContextFidelity.FULL_CHAIN_AT_FILL)), ContextFidelity.FULL_CHAIN_AT_FILL)
                .summary();

        assertThat(summary).contains("have not written down");
    }

    @Test
    @DisplayName("unexplained winners are separable, since they are the strongest evidence")
    void unexplainedWinnersAreSeparable() {
        JournalEntry winner = entry(snapshotMoving("101.00", "100.00"),
                ContextFidelity.FULL_CHAIN_AT_FILL)
                .closes(1L, new BigDecimal("2.5"), NOW);
        JournalEntry loser = entry(snapshotMoving("101.50", "100.00"),
                ContextFidelity.FULL_CHAIN_AT_FILL)
                .closes(2L, new BigDecimal("-1.0"), NOW);

        DisagreementReport report = analyzer()
                .analyse(List.of(winner, loser), ContextFidelity.FULL_CHAIN_AT_FILL);

        // A profitable trade no rule describes is the strongest signal that
        // something real is missing - but still only a signal, which is why the
        // next step is a candidate rule and a backtest.
        assertThat(report.unexplainedTrades()).hasSize(2);
        assertThat(report.unexplainedWinners()).hasSize(1);
        assertThat(report.unexplainedWinners().get(0).rMultiple()).isEqualByComparingTo("2.5");
    }

    @Test
    @DisplayName("the report records which fidelity tier produced it")
    void reportCarriesItsFidelity() {
        DisagreementReport report = analyzer()
                .analyse(List.of(entry(snapshotMoving("105.00", "100.00"),
                        ContextFidelity.UNDERLYING_ONLY)), ContextFidelity.UNDERLYING_ONLY);

        // A reader must not be able to mistake which observations these were.
        assertThat(report.fidelity()).isEqualTo(ContextFidelity.UNDERLYING_ONLY);
        assertThat(report.summary()).contains("UNDERLYING_ONLY");
    }

    @Test
    @DisplayName("the trader's own judgement travels with the comparison, separate from the outcome")
    void judgementIsCarriedSeparately() {
        JournalEntry judged = entry(snapshotMoving("101.00", "100.00"),
                ContextFidelity.FULL_CHAIN_AT_FILL)
                .closes(1L, new BigDecimal("3.0"), NOW)
                .judge(Judgement.WOULD_NOT_REPEAT, NOW);

        Disagreement result = analyzer().compare(judged);

        // A winner the trader would not repeat is exactly the case that a model
        // fitted to P&L would learn the wrong lesson from.
        assertThat(result.judgement()).isEqualTo(Judgement.WOULD_NOT_REPEAT);
        assertThat(result.rMultiple()).isEqualByComparingTo("3.0");
    }

    @Test
    @DisplayName("an empty journal reports that there is nothing to compare")
    void emptyJournalIsSafe() {
        DisagreementReport report =
                analyzer().analyse(List.of(), ContextFidelity.FULL_CHAIN_AT_FILL);

        assertThat(report.disagreements()).isEmpty();
        assertThat(report.summary()).contains("No entries");
    }

    @Test
    @DisplayName("silent trades are grouped by underlying, so a single missing setup shows")
    void silentTradesGroupByUnderlying() {
        DisagreementReport report = analyzer().analyse(List.of(
                entry(snapshotMoving("101.00", "100.00"), ContextFidelity.FULL_CHAIN_AT_FILL),
                entry(snapshotMoving("101.50", "100.00"), ContextFidelity.FULL_CHAIN_AT_FILL)),
                ContextFidelity.FULL_CHAIN_AT_FILL);

        // One ticker dominating usually means one setup is missing, not many.
        assertThat(report.silentByUnderlying()).containsEntry("AAPL", 2L);
    }

    @Test
    @DisplayName("the analyzer holds no repository, so mining cannot mutate the journal")
    void analyzerCannotWrite() {
        String fields = java.util.Arrays.stream(DisagreementAnalyzer.class.getDeclaredFields())
                .map(f -> f.getType().getName())
                .reduce("", (a, b) -> a + " " + b);

        assertThat(fields).doesNotContain("Repository").doesNotContain("EntityManager");
    }
}
