package com.moneyteam.bot.journal.analysis;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * A stated rule, as something checkable.
 *
 * The grammar is one comparison and nothing else:
 *
 * <pre>
 *   changePercent &gt;= 4
 *   volumeToOpenInterest &gt; 0.5
 *   close &gt; previousClose
 * </pre>
 *
 * <h2>Why it is this small</h2>
 * No boolean operators, no arithmetic, no nesting. The purpose is not to build
 * a rule engine - the strategy rules already exist in Java, where they are
 * tested. The purpose is to take a sentence someone believes about their own
 * trading and count how often their fills contradict it, and for that a single
 * comparison is enough. A richer grammar would add parsing bugs to a component
 * whose whole value is being obviously correct.
 *
 * A rule naming a feature that cannot be computed is {@link #isCheckable()
 * uncheckable} rather than failing or passing. Treating it as satisfied would
 * read as compliance, which is the one answer worse than no answer.
 */
public record RuleExpression(String feature, Operator operator, String comparandFeature,
                             BigDecimal comparandValue, String raw) {

    public enum Operator {
        GT(">"), GTE(">="), LT("<"), LTE("<="), EQ("=="), NEQ("!=");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        public String symbol() {
            return symbol;
        }

        boolean holds(int comparison) {
            return switch (this) {
                case GT -> comparison > 0;
                case GTE -> comparison >= 0;
                case LT -> comparison < 0;
                case LTE -> comparison <= 0;
                case EQ -> comparison == 0;
                case NEQ -> comparison != 0;
            };
        }
    }

    /**
     * Parses one comparison.
     *
     * Operators are tried longest-first, or {@code >=} would match {@code >}
     * and leave "= 4" as the right-hand side.
     */
    public static RuleExpression parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("A rule expression is required.");
        }
        String text = raw.trim();

        for (Operator operator : new Operator[]{
                Operator.GTE, Operator.LTE, Operator.EQ, Operator.NEQ, Operator.GT, Operator.LT}) {

            int at = text.indexOf(operator.symbol());
            if (at < 0) {
                continue;
            }

            String left = text.substring(0, at).trim();
            String right = text.substring(at + operator.symbol().length()).trim();
            if (left.isEmpty() || right.isEmpty()) {
                break;
            }

            if (!SnapshotFeatures.knownFeatures().contains(left)) {
                throw new IllegalArgumentException(
                        "Unknown feature '%s'. Known features: %s".formatted(
                                left, SnapshotFeatures.knownFeatures()));
            }

            // The right side is either another feature or a number.
            if (SnapshotFeatures.knownFeatures().contains(right)) {
                return new RuleExpression(left, operator, right, null, text);
            }
            try {
                return new RuleExpression(left, operator, null, new BigDecimal(right), text);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "'%s' is neither a number nor a known feature.".formatted(right), e);
            }
        }

        throw new IllegalArgumentException(
                "Could not parse '%s'. Expected one comparison, for example "
                        + "'changePercent >= 4' or 'close > previousClose'.".formatted(text));
    }

    /**
     * Whether the rule holds for these features.
     *
     * Empty when a named feature is absent - which is not the same as false.
     * See the class note.
     */
    public Optional<Boolean> evaluate(Map<String, BigDecimal> features) {
        BigDecimal left = features.get(feature);
        if (left == null) {
            return Optional.empty();
        }

        BigDecimal right = comparandFeature != null
                ? features.get(comparandFeature)
                : comparandValue;
        if (right == null) {
            return Optional.empty();
        }

        return Optional.of(operator.holds(left.compareTo(right)));
    }

    /** True when both sides can be computed from today's feature set. */
    public boolean isCheckable() {
        return SnapshotFeatures.knownFeatures().contains(feature)
                && (comparandValue != null
                    || SnapshotFeatures.knownFeatures().contains(comparandFeature));
    }

    @Override
    public String toString() {
        return raw;
    }
}
