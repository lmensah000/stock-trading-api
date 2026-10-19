package com.moneyteam.bot.journal.analysis;

import com.moneyteam.bot.journal.ContextFidelity;
import com.moneyteam.bot.journal.ContextSnapshot;
import com.moneyteam.bot.journal.JournalEntry;
import com.moneyteam.bot.journal.SnapshotCodec;
import com.moneyteam.strategy.model.Signal;
import com.moneyteam.strategy.rule.EvaluationContext;
import com.moneyteam.strategy.rule.StrategyRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Replays the encoded rules over recorded fills and reports where they and the
 * trader disagreed.
 *
 * <h2>Why this and not a model</h2>
 * This needs no training data, no sample size and no options vendor, and it
 * produces something usable from the first handful of trades. The set of trades
 * taken while every rule stayed silent <em>is</em> the list of things known but
 * never written down - no inference required. A model fitted to twenty option
 * trades would mostly be describing noise; this is arithmetic over a
 * comparison.
 *
 * <h2>One tier at a time</h2>
 * Analysis is scoped to a single {@link ContextFidelity}. An imported fill
 * carries no chain, so the option rules cannot fire on it - pooling tiers would
 * make those entries look like evidence the option rules never agree with the
 * trader, when in fact they were never given anything to read.
 *
 * Holds no repository, so it cannot write anything: this reads history and
 * reports, and a component that mines a journal has no business mutating one.
 */
public class DisagreementAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(DisagreementAnalyzer.class);

    private final List<StrategyRule> rules;
    private final SnapshotCodec codec;

    public DisagreementAnalyzer(List<StrategyRule> rules, SnapshotCodec codec) {
        this.rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    /**
     * Compares every entry in one fidelity tier.
     *
     * @param entries   journal entries, already filtered to one tier
     * @param fidelity  the tier, carried into the report so a reader cannot
     *                  mistake which observations produced it
     */
    public DisagreementReport analyse(List<JournalEntry> entries, ContextFidelity fidelity) {
        List<Disagreement> results = new ArrayList<>(entries.size());

        for (JournalEntry entry : entries) {
            results.add(compare(entry));
        }

        DisagreementReport report = DisagreementReport.of(results, fidelity, rules.size());
        log.info("Disagreement analysis over {} entries at fidelity {}: {}",
                entries.size(), fidelity, report.summary());
        return report;
    }

    /**
     * Compares one fill against the rules.
     *
     * Every entry in the journal was, by definition, a trade that was taken -
     * so {@link Disagreement.Verdict#RULES_FIRED_YOU_PASSED} cannot arise from
     * a journal entry alone. It is produced by
     * {@link #comparePassedSetup(String, ContextSnapshot, ContextFidelity)},
     * which takes a setup the bot flagged and the trader did not act on.
     */
    public Disagreement compare(JournalEntry entry) {
        Optional<ContextSnapshot> snapshot = readSnapshot(entry);

        if (snapshot.isEmpty() || !snapshot.get().isUsable()) {
            // Reported, not dropped. A silently skipped entry would make the
            // other counts look like they covered the whole journal.
            return new Disagreement(
                    entry.getId(), entry.getUnderlying(), entry.getFilledAt(),
                    Disagreement.Verdict.NOT_EVALUABLE, List.of(),
                    entry.getContextFidelity(), entry.getJudgement(), entry.getRMultiple());
        }

        List<String> fired = firedRules(snapshot.get().toEvaluationContext());

        return new Disagreement(
                entry.getId(), entry.getUnderlying(), entry.getFilledAt(),
                fired.isEmpty()
                        ? Disagreement.Verdict.YOU_TOOK_RULES_SILENT
                        : Disagreement.Verdict.AGREED,
                fired, entry.getContextFidelity(), entry.getJudgement(), entry.getRMultiple());
    }

    /**
     * A setup the rules flagged that was not traded.
     *
     * Kept separate because its input is not a journal entry - there is no fill
     * to record. It is the only window onto what gets correctly avoided, and it
     * is a narrow one: it sees only setups the rules already recognise, never
     * the ones nobody looked at.
     */
    public Disagreement comparePassedSetup(String underlying,
                                           ContextSnapshot snapshot,
                                           ContextFidelity fidelity) {
        List<String> fired = snapshot.isUsable()
                ? firedRules(snapshot.toEvaluationContext())
                : List.of();

        return new Disagreement(
                null, underlying, snapshot.capturedAt(),
                fired.isEmpty()
                        ? Disagreement.Verdict.NOT_EVALUABLE
                        : Disagreement.Verdict.RULES_FIRED_YOU_PASSED,
                fired, fidelity, null, null);
    }

    private List<String> firedRules(EvaluationContext context) {
        List<String> fired = new ArrayList<>();
        for (StrategyRule rule : rules) {
            try {
                Optional<Signal> signal = rule.evaluate(context);
                if (signal.isPresent()) {
                    fired.add(rule.name());
                }
            } catch (RuntimeException e) {
                // One failing rule must not invalidate the comparison for the
                // others; a rule that cannot read a thin snapshot is expected.
                log.debug("Rule {} could not evaluate the snapshot: {}", rule.name(), e.toString());
            }
        }
        return fired;
    }

    private Optional<ContextSnapshot> readSnapshot(JournalEntry entry) {
        if (entry.getSnapshotJson() == null || entry.getSnapshotJson().isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(codec.read(entry.getSnapshotJson()));
        } catch (Exception e) {
            log.warn("Snapshot for journal entry {} could not be read: {}",
                    entry.getId(), e.toString());
            return Optional.empty();
        }
    }
}
