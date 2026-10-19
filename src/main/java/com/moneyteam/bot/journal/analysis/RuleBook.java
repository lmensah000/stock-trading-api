package com.moneyteam.bot.journal.analysis;

import com.moneyteam.bot.journal.ContextSnapshot;
import com.moneyteam.bot.journal.JournalEntry;
import com.moneyteam.bot.journal.SnapshotCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Measures stated rules against recorded fills.
 *
 * The rules here are not obeyed by anything - they are not the strategy. They
 * are what the trader believes about their own trading, and their value is
 * entirely in the contradiction with what the journal shows.
 *
 * Deliberately separate from {@code StrategyRule}: those are executable
 * conditions that produce signals, these are claims to be audited. Conflating
 * them would mean a belief someone had not yet tested could start influencing
 * trades.
 */
public class RuleBook {

    private static final Logger log = LoggerFactory.getLogger(RuleBook.class);

    private final SnapshotCodec codec;

    public RuleBook(SnapshotCodec codec) {
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    /** A rule as stated, paired with its checkable form. */
    public record StatedRule(String statement, RuleExpression expression) {

        public static StatedRule of(String statement, String expression) {
            return new StatedRule(statement, RuleExpression.parse(expression));
        }
    }

    /**
     * Checks every rule against every fill.
     *
     * A rule whose feature is missing from a snapshot counts as uncheckable
     * for that fill, never as satisfied. Counting absence as compliance would
     * make a rule look upheld precisely where there was no evidence either way.
     */
    public RuleConsistencyReport check(List<StatedRule> rules, List<JournalEntry> entries) {
        // Features computed once per entry rather than once per rule-entry pair.
        List<Map<String, BigDecimal>> featuresPerEntry = new ArrayList<>(entries.size());
        for (JournalEntry entry : entries) {
            featuresPerEntry.add(featuresOf(entry));
        }

        List<RuleConsistencyReport.RuleOutcome> outcomes = new ArrayList<>(rules.size());

        for (StatedRule rule : rules) {
            int checked = 0;
            int violations = 0;
            int uncheckable = 0;

            for (Map<String, BigDecimal> features : featuresPerEntry) {
                Optional<Boolean> held = rule.expression().evaluate(features);
                if (held.isEmpty()) {
                    uncheckable++;
                } else {
                    checked++;
                    if (!held.get()) {
                        violations++;
                    }
                }
            }

            outcomes.add(new RuleConsistencyReport.RuleOutcome(
                    rule.statement(), rule.expression().toString(),
                    checked, violations, uncheckable));
        }

        RuleConsistencyReport report = new RuleConsistencyReport(List.copyOf(outcomes));
        log.info("Rule consistency over {} fill(s): {}", entries.size(), report.summary());
        return report;
    }

    private Map<String, BigDecimal> featuresOf(JournalEntry entry) {
        if (entry.getSnapshotJson() == null || entry.getSnapshotJson().isBlank()) {
            return Map.of();
        }
        try {
            ContextSnapshot snapshot = codec.read(entry.getSnapshotJson());
            Map<String, BigDecimal> features = new LinkedHashMap<>(SnapshotFeatures.of(snapshot));

            // Chain features are dropped when the fidelity does not carry a
            // chain. Without this an imported fill could be checked against an
            // open-interest rule using whatever happened to be in the snapshot,
            // which is a different moment's data.
            if (!entry.hasChainContext()) {
                features.remove(SnapshotFeatures.CONTRACT_VOLUME);
                features.remove(SnapshotFeatures.OPEN_INTEREST);
                features.remove(SnapshotFeatures.VOLUME_TO_OPEN_INTEREST);
            }
            return Map.copyOf(features);
        } catch (RuntimeException e) {
            log.debug("Could not read the snapshot for entry {}: {}", entry.getId(), e.toString());
            return Map.of();
        }
    }
}
