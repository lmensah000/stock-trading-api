package com.moneyteam.bot.backtest;

import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.strategy.model.Signal;
import com.moneyteam.strategy.rule.EvaluationContext;
import com.moneyteam.strategy.rule.StrategyRule;
import com.moneyteam.trading.service.PositionSizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Replays a bar series through the real strategy rules and scores the outcomes
 * in R.
 *
 * <h2>No lookahead, structurally</h2>
 * At step {@code i} the rule is handed {@code bars.subList(0, i + 1)}. Later
 * bars are not ignored by convention - they are absent from the object the rule
 * receives, so a rule physically cannot read a future price even if it tried.
 * Entry is at bar {@code i+1}'s open, never bar {@code i}'s close, because the
 * close that produced the signal is only known once that bar is over.
 *
 * <h2>Why not StrategyEngine</h2>
 * Two concrete reasons, not preference. {@code StrategyEngine.loadContext}
 * stamps the context with {@code Instant.now()}, and every rule reads
 * {@code evaluatedAt()} for its signal timestamp - so replaying through the
 * engine would give historical signals wall-clock times. And the engine exists
 * to parallelise across tickers, whereas a backtest has to be deterministic;
 * {@code CLAUDE.md} already requires that anything acting on signals be
 * single-threaded. Same rules, same detectors, correct clock.
 *
 * <h2>Touches no database</h2>
 * No repository is injected and none is reachable. A replay that booked
 * simulated fills into {@code Account} would feed the live-trading gate, which
 * reads equity to decide whether real money becomes reachable.
 *
 * See {@code docs/BACKTEST-SPEC.md}, which fixes this behaviour with
 * hand-computed numbers and was written before this class.
 */
public final class BacktestRunner {

    private static final Logger log = LoggerFactory.getLogger(BacktestRunner.class);
    private static final int SCALE = 4;
    private static final BigDecimal TARGET_R_MULTIPLE = BigDecimal.valueOf(2);

    private final List<StrategyRule> rules;
    private final CostModel costModel;
    private final BigDecimal equity;
    private final BigDecimal riskPercent;
    private final int minimumTrades;

    public BacktestRunner(List<StrategyRule> rules,
                          CostModel costModel,
                          BigDecimal equity,
                          BigDecimal riskPercent,
                          int minimumTrades) {
        this.rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
        this.costModel = Objects.requireNonNull(costModel, "costModel");
        this.equity = Objects.requireNonNull(equity, "equity");
        this.riskPercent = Objects.requireNonNull(riskPercent, "riskPercent");
        this.minimumTrades = minimumTrades;

        if (this.rules.isEmpty()) {
            throw new IllegalArgumentException("at least one rule is required");
        }
    }

    /**
     * Replays one ticker's series.
     *
     * One position at a time. Overlapping entries would mean the exposure
     * ceiling and the risk budget were both being exceeded silently, so a
     * signal arriving while a position is open is counted and skipped.
     */
    public BacktestReport run(String ticker, Timeframe timeframe, List<Bar> series) {
        Bar.requireOldestFirst(series);

        List<BacktestTrade> trades = new ArrayList<>();
        int signalsFound = 0;
        int i = 0;

        // The last bar can produce a signal but never an entry: there is no
        // i+1 to enter on. Stopping at size()-2 states that rather than
        // discovering it as an index error.
        while (i <= series.size() - 2) {
            Optional<Signal> signal = evaluateAt(ticker, timeframe, series, i);
            if (signal.isEmpty()) {
                i++;
                continue;
            }
            signalsFound++;

            Optional<BacktestTrade> trade = openAndResolve(ticker, series, i);
            if (trade.isEmpty()) {
                // Size came out at zero shares, or the stop was not below the
                // trigger so risk was undefined. Not a trade.
                i++;
                continue;
            }

            trades.add(trade.get());

            // Resume after the exit bar: one position at a time.
            i = indexOf(series, trade.get().exitAt()) + 1;
        }

        BacktestReport report = BacktestReport.of(
                trades, series.size(), signalsFound, minimumTrades, costModel);
        log.info("Backtest {} {}: {}", ticker, timeframe, report.summary());
        return report;
    }

    /**
     * Builds the context from the truncated series and evaluates every rule.
     *
     * {@code previousClose} is taken from {@code bars[i-1]}, never from the
     * provider: for a CSV file the provider's answer is the last bar in the
     * file, which at step {@code i} is a future price. That is lookahead
     * arriving through an interface that looks innocent.
     */
    private Optional<Signal> evaluateAt(String ticker, Timeframe timeframe, List<Bar> series, int i) {
        List<Bar> visible = series.subList(0, i + 1);

        EvaluationContext.Builder builder =
                EvaluationContext.builder(ticker, series.get(i).openTime())
                        .bars(timeframe, visible)
                        .accountEquity(equity);

        if (i >= 1) {
            builder.previousClose(series.get(i - 1).close());
        }

        EvaluationContext context = builder.build();

        for (StrategyRule rule : rules) {
            Optional<Signal> found = rule.evaluate(context);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /** Enters at bar {@code i+1}'s open and walks forward until an exit. */
    private Optional<BacktestTrade> openAndResolve(String ticker, List<Bar> series, int i) {
        Bar signalBar = series.get(i);
        Bar entryBar = series.get(i + 1);

        BigDecimal trigger = entryBar.open();
        BigDecimal stop = signalBar.low();

        if (stop.compareTo(trigger) >= 0) {
            // A gap down through the signal bar's low leaves no defined risk.
            return Optional.empty();
        }

        long shares;
        try {
            shares = PositionSizer.sharesForRisk(equity, riskPercent, trigger, stop);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        if (shares <= 0) {
            return Optional.empty();
        }

        BigDecimal plannedRisk = trigger.subtract(stop);
        BigDecimal target = PositionSizer.firstTarget(trigger, stop, TARGET_R_MULTIPLE);

        for (int j = i + 1; j < series.size(); j++) {
            Bar bar = series.get(j);
            boolean stopHit = bar.low().compareTo(stop) <= 0;
            boolean targetHit = bar.high().compareTo(target) >= 0;

            if (stopHit && targetHit) {
                // Both inside one bar. OHLC does not say which came first, so
                // the stop is taken. Resolving this the other way turns losing
                // systems into winning ones on paper, because the ambiguous
                // bars are exactly the volatile ones momentum rules produce.
                return Optional.of(close(ticker, signalBar, entryBar, bar, trigger, stop, target,
                        shares, plannedRisk, stop, BacktestTrade.Outcome.STOP_AMBIGUOUS_BAR));
            }
            if (stopHit) {
                return Optional.of(close(ticker, signalBar, entryBar, bar, trigger, stop, target,
                        shares, plannedRisk, stop, BacktestTrade.Outcome.STOP));
            }
            if (targetHit) {
                return Optional.of(close(ticker, signalBar, entryBar, bar, trigger, stop, target,
                        shares, plannedRisk, target, BacktestTrade.Outcome.TARGET));
            }
        }

        // Data ran out with the position open. Marked to the last close and
        // labelled, rather than dropped: silently discarding open positions
        // removes whichever trades were still losing at the end of the series.
        Bar last = series.get(series.size() - 1);
        return Optional.of(close(ticker, signalBar, entryBar, last, trigger, stop, target,
                shares, plannedRisk, last.close(), BacktestTrade.Outcome.END_OF_DATA));
    }

    private BacktestTrade close(String ticker,
                                Bar signalBar,
                                Bar entryBar,
                                Bar exitBar,
                                BigDecimal trigger,
                                BigDecimal stop,
                                BigDecimal target,
                                long shares,
                                BigDecimal plannedRisk,
                                BigDecimal exitPrice,
                                BacktestTrade.Outcome outcome) {

        BigDecimal entryFill = costModel.buyFill(trigger);
        BigDecimal exitFill = costModel.sellFill(exitPrice);

        BigDecimal realisedPerShare = exitFill
                .subtract(entryFill)
                .subtract(costModel.roundTripCommissionPerShare())
                .setScale(SCALE, RoundingMode.HALF_UP);

        // plannedRisk, not the actual fill spread: R means multiples of what was
        // risked at plan time, which is what the position was sized from.
        BigDecimal rMultiple = realisedPerShare.divide(plannedRisk, SCALE, RoundingMode.HALF_UP);

        return new BacktestTrade(
                ticker, signalBar.openTime(), entryBar.openTime(), exitBar.openTime(),
                trigger, stop, target, shares, entryFill, exitFill,
                plannedRisk, realisedPerShare, rMultiple, outcome);
    }

    private static int indexOf(List<Bar> series, java.time.Instant openTime) {
        for (int i = 0; i < series.size(); i++) {
            if (series.get(i).openTime().equals(openTime)) {
                return i;
            }
        }
        throw new IllegalStateException("exit bar not found in series: " + openTime);
    }
}
