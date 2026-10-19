package com.moneyteam.bot.journal.analysis;

import com.moneyteam.bot.journal.ContextFidelity;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * What the comparison found, for one fidelity tier.
 *
 * The number to read first is {@link #rulesSilentCount()}: trades taken while
 * every encoded rule stayed quiet. Each one is a decision made for a reason the
 * system does not contain, and the annotated charts attached to those entries
 * are the shortest path to finding out what the reason was.
 *
 * <h2>What this report is not</h2>
 * It is not a measure of whether the rules are any good. A rule that fires on
 * every trade agrees with everything and discriminates nothing. Agreement here
 * means only "the rules also noticed", and whether noticing was worth anything
 * is a question for the backtest.
 */
public record DisagreementReport(
        List<Disagreement> disagreements,
        ContextFidelity fidelity,
        int ruleCount,
        Map<Disagreement.Verdict, Long> countsByVerdict,
        Map<String, Long> silentByUnderlying) {

    public static DisagreementReport of(List<Disagreement> disagreements,
                                        ContextFidelity fidelity,
                                        int ruleCount) {

        List<Disagreement> all = List.copyOf(disagreements);

        Map<Disagreement.Verdict, Long> counts = all.stream()
                .collect(Collectors.groupingBy(Disagreement::verdict, Collectors.counting()));

        // Which instruments produce the most unexplained trades. A single
        // ticker dominating usually means one setup is missing, not many.
        Map<String, Long> silent = all.stream()
                .filter(d -> d.verdict() == Disagreement.Verdict.YOU_TOOK_RULES_SILENT)
                .collect(Collectors.groupingBy(Disagreement::underlying, Collectors.counting()));

        return new DisagreementReport(all, fidelity, ruleCount, counts, silent);
    }

    public long countOf(Disagreement.Verdict verdict) {
        return countsByVerdict.getOrDefault(verdict, 0L);
    }

    /** Trades taken while no rule fired. The articulation gap, as a number. */
    public long rulesSilentCount() {
        return countOf(Disagreement.Verdict.YOU_TOOK_RULES_SILENT);
    }

    /** Entries whose snapshot was missing or too thin to replay. */
    public long notEvaluableCount() {
        return countOf(Disagreement.Verdict.NOT_EVALUABLE);
    }

    /** The entries worth attaching a chart to and looking at. */
    public List<Disagreement> unexplainedTrades() {
        return disagreements.stream()
                .filter(d -> d.verdict() == Disagreement.Verdict.YOU_TOOK_RULES_SILENT)
                .toList();
    }

    /**
     * Trades the rules missed <em>and</em> that worked.
     *
     * The most valuable subset: a profitable trade no rule describes is the
     * strongest evidence that something real is missing. Still only evidence -
     * a handful of winners can be luck, which is why the next step is a
     * candidate rule and a backtest rather than a new rule straight into the
     * live set.
     */
    public List<Disagreement> unexplainedWinners() {
        return unexplainedTrades().stream()
                .filter(d -> d.rMultiple() != null && d.rMultiple().signum() > 0)
                .toList();
    }

    public String summary() {
        if (disagreements.isEmpty()) {
            return "No entries at fidelity " + fidelity + " to compare.";
        }

        long silent = rulesSilentCount();
        long agreed = countOf(Disagreement.Verdict.AGREED);
        long passed = countOf(Disagreement.Verdict.RULES_FIRED_YOU_PASSED);
        long unusable = notEvaluableCount();

        StringBuilder text = new StringBuilder(
                "%d entr%s at %s against %d rule(s): %d agreed, %d taken with every rule silent"
                        .formatted(disagreements.size(), disagreements.size() == 1 ? "y" : "ies",
                                fidelity, ruleCount, agreed, silent));

        if (passed > 0) {
            text.append(", ").append(passed).append(" flagged but passed over");
        }
        if (unusable > 0) {
            text.append(", ").append(unusable).append(" not evaluable");
        }
        if (silent > 0) {
            text.append(". The ").append(silent)
                    .append(" silent one(s) are the rules you have not written down yet");
        }
        return text.append('.').toString();
    }
}
