package com.moneyteam.bot.journal.analysis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * How often a trader's fills contradict a rule they believe they follow.
 *
 * This is arithmetic, not inference, and it is often where someone discovers
 * the rule they actually trade. "You said never below the previous close; 40%
 * of your fills were below it" cannot be argued with, and it does not require
 * the trader to have been able to describe anything.
 *
 * <h2>A high violation rate is not necessarily a problem</h2>
 * It means one of two things, and the report cannot tell them apart: either the
 * rule is being broken, or the rule as stated is not the rule being traded. The
 * second is the more interesting case and the more common one. So this reports
 * the contradiction and stops - deciding which it is belongs to the person.
 */
public record RuleConsistencyReport(List<RuleOutcome> outcomes) {

    /**
     * One rule's result.
     *
     * @param statement   the rule as stated, in plain English
     * @param expression  the checkable form
     * @param checked     fills the rule could actually be evaluated against
     * @param violations  fills that contradicted it
     * @param uncheckable fills whose snapshot lacked a feature the rule names
     */
    public record RuleOutcome(String statement,
                              String expression,
                              int checked,
                              int violations,
                              int uncheckable) {

        /** Share of checkable fills that contradicted the rule, 0 to 1. */
        public BigDecimal violationRate() {
            if (checked == 0) {
                return null;
            }
            return BigDecimal.valueOf(violations)
                    .divide(BigDecimal.valueOf(checked), 4, RoundingMode.HALF_UP);
        }

        public String describe() {
            if (checked == 0) {
                return ("\"%s\" could not be checked against any fill. "
                        + "%d snapshot(s) lacked a feature it names - most likely it needs an "
                        + "indicator that does not exist yet.")
                        .formatted(statement, uncheckable);
            }

            BigDecimal rate = violationRate();
            if (violations == 0) {
                return "\"%s\" held on all %d checkable fill(s).".formatted(statement, checked);
            }

            return ("\"%s\" was contradicted by %d of %d fill(s) (%s%%). Either it is being "
                    + "broken, or it is not the rule actually being traded.")
                    .formatted(statement, violations, checked,
                            rate.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP));
        }
    }

    /** Rules contradicted by at least one fill, worst first. */
    public List<RuleOutcome> contradicted() {
        return outcomes.stream()
                .filter(o -> o.violations() > 0)
                .sorted((a, b) -> b.violations() - a.violations())
                .toList();
    }

    /** Rules that could not be evaluated at all, usually for want of an indicator. */
    public List<RuleOutcome> uncheckable() {
        return outcomes.stream().filter(o -> o.checked() == 0).toList();
    }

    public String summary() {
        if (outcomes.isEmpty()) {
            return "No rules stated.";
        }
        long contradicted = contradicted().size();
        long unchecked = uncheckable().size();

        return "%d rule(s): %d contradicted by at least one fill, %d not checkable%s".formatted(
                outcomes.size(), contradicted, unchecked,
                contradicted > 0
                        ? ". The contradicted ones are where a stated rule and a traded rule differ."
                        : ".");
    }
}
