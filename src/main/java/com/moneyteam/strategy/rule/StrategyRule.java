package com.moneyteam.strategy.rule;

import com.moneyteam.strategy.model.Signal;

import java.util.Optional;

/**
 * One condition the engine evaluates against one ticker.
 *
 * Implementations must be <strong>pure and thread-safe</strong>: a rule is
 * applied to many tickers concurrently, and the same instance is shared across
 * those threads. Any per-ticker state belongs in the {@link EvaluationContext}
 * passed in, never in a field.
 *
 * <p>A rule reports what it observed. It does not decide what to do about it,
 * does not raise alerts, and does not place orders - the engine collects
 * signals and the layers above decide. Keeping that boundary is what allows a
 * rule to be tested with nothing but a context and an assertion.
 */
@FunctionalInterface
public interface StrategyRule {

    /**
     * @return a signal when the condition is met, otherwise empty
     */
    Optional<Signal> evaluate(EvaluationContext context);

    /** Human-readable name, used in logs and failure reporting. */
    default String name() {
        return getClass().getSimpleName();
    }
}
