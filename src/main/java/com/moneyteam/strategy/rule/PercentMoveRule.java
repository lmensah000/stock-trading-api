package com.moneyteam.strategy.rule;

import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.strategy.model.Confidence;
import com.moneyteam.strategy.model.Direction;
import com.moneyteam.strategy.model.Signal;
import com.moneyteam.strategy.model.SignalType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Fires when a ticker has moved by at least a configured percentage.
 *
 * <h2>Baseline is configuration, not a second rule</h2>
 * "Up 4% from yesterday's close" and "up 2% in the last half hour" are the same
 * question measured from different starting points. Making the baseline a
 * parameter keeps them one code path with one set of tests, rather than two
 * implementations that drift apart.
 *
 * <h2>What this rule deliberately does not do</h2>
 * It reports a move. It does not conclude a trade is warranted. A percentage
 * move is a <em>setup</em> signal, and the strategy this app implements is
 * explicit that market stage comes first and that a setup on a stock with no
 * relative strength is not a trade. Treating this signal as sufficient on its
 * own would invert that order of operations.
 *
 * Immutable and thread-safe; one instance serves every ticker in a scan.
 */
public final class PercentMoveRule implements StrategyRule {

    /** Where the move is measured from. */
    public enum Baseline {
        /** Previous session's closing price - an overnight-inclusive move. */
        PREVIOUS_CLOSE,
        /** The open of the earliest loaded bar - an intraday move. */
        SESSION_OPEN,
        /** The close N bars ago - a recent-momentum move. */
        N_BARS_AGO
    }

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final BigDecimal thresholdPercent;
    private final Baseline baseline;
    private final Timeframe timeframe;
    private final int barsAgo;
    private final boolean includeDownMoves;

    /**
     * @param thresholdPercent magnitude that must be met, e.g. 4 for 4%
     * @param baseline         what the move is measured from
     * @param timeframe        which bar series to read
     * @param barsAgo          lookback for {@link Baseline#N_BARS_AGO}; ignored otherwise
     * @param includeDownMoves when false only upward moves fire
     */
    public PercentMoveRule(BigDecimal thresholdPercent,
                           Baseline baseline,
                           Timeframe timeframe,
                           int barsAgo,
                           boolean includeDownMoves) {
        this.thresholdPercent = Objects.requireNonNull(thresholdPercent, "thresholdPercent");
        this.baseline = Objects.requireNonNull(baseline, "baseline");
        this.timeframe = Objects.requireNonNull(timeframe, "timeframe");
        this.barsAgo = barsAgo;
        this.includeDownMoves = includeDownMoves;

        if (thresholdPercent.signum() <= 0) {
            throw new IllegalArgumentException("thresholdPercent must be positive: " + thresholdPercent);
        }
        if (baseline == Baseline.N_BARS_AGO && barsAgo < 1) {
            throw new IllegalArgumentException("barsAgo must be at least 1 for N_BARS_AGO");
        }
    }

    /** An upward-only move from the previous close - the common configuration. */
    public static PercentMoveRule upFromPreviousClose(BigDecimal thresholdPercent) {
        return new PercentMoveRule(thresholdPercent, Baseline.PREVIOUS_CLOSE, Timeframe.M5, 0, false);
    }

    @Override
    public Optional<Signal> evaluate(EvaluationContext context) {
        Bar latest = context.latestBar(timeframe);
        if (latest == null) {
            return Optional.empty();
        }

        BigDecimal from = resolveBaseline(context);
        if (from == null || from.signum() == 0) {
            // No baseline, or a zero baseline that would make the percentage
            // undefined. Reporting nothing beats reporting a fabricated number.
            return Optional.empty();
        }

        BigDecimal current = latest.close();
        BigDecimal changePercent = current.subtract(from)
                .divide(from, 6, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .setScale(4, RoundingMode.HALF_UP);

        boolean up = changePercent.signum() >= 0;
        if (!up && !includeDownMoves) {
            return Optional.empty();
        }

        // Inclusive: a move of exactly the threshold has met it.
        if (changePercent.abs().compareTo(thresholdPercent) < 0) {
            return Optional.empty();
        }

        Direction direction = up ? Direction.BULLISH : Direction.BEARISH;

        Map<String, String> evidence = new LinkedHashMap<>();
        evidence.put("baseline", baseline.name());
        evidence.put("baselinePrice", from.toPlainString());
        evidence.put("currentPrice", current.toPlainString());
        evidence.put("changePercent", changePercent.toPlainString());
        evidence.put("thresholdPercent", thresholdPercent.toPlainString());
        evidence.put("timeframe", timeframe.name());
        if (baseline == Baseline.N_BARS_AGO) {
            evidence.put("barsAgo", Integer.toString(barsAgo));
        }

        String summary = "%s moved %s%% from %s (threshold %s%%)".formatted(
                context.ticker(),
                changePercent.toPlainString(),
                baseline.name().toLowerCase().replace('_', ' '),
                thresholdPercent.toPlainString());

        return Optional.of(new Signal(
                SignalType.PERCENT_MOVE,
                context.ticker(),
                direction,
                // A price move on its own is corroborated by nothing else.
                Confidence.MEDIUM,
                context.evaluatedAt(),
                summary,
                dedupeKey(context, direction),
                evidence));
    }

    private BigDecimal resolveBaseline(EvaluationContext context) {
        return switch (baseline) {
            case PREVIOUS_CLOSE -> context.previousClose();
            case SESSION_OPEN -> {
                List<Bar> series = context.bars(timeframe);
                yield series.isEmpty() ? null : series.get(0).open();
            }
            case N_BARS_AGO -> {
                List<Bar> series = context.bars(timeframe);
                int index = series.size() - 1 - barsAgo;
                yield index < 0 ? null : series.get(index).close();
            }
        };
    }

    /**
     * Keyed on the condition rather than the observation: the same ticker
     * moving the same way past the same threshold is one condition, however
     * many scans observe it.
     */
    private String dedupeKey(EvaluationContext context, Direction direction) {
        return "PERCENT_MOVE:%s:%s:%s:%s".formatted(
                context.ticker(), direction, baseline, thresholdPercent.toPlainString());
    }

    @Override
    public String name() {
        return "PercentMoveRule(%s%% from %s)".formatted(thresholdPercent.toPlainString(), baseline);
    }
}
