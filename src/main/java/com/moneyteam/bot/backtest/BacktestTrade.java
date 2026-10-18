package com.moneyteam.bot.backtest;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One completed round trip in a backtest.
 *
 * A plain record, not a JPA entity. A backtest must not write {@code Account},
 * {@code LedgerEntry}, {@code Position} or {@code TradePlan}: the live-trading
 * gate reads account equity to decide whether real money becomes reachable, so
 * a replay that booked simulated fills into those tables could arm the gate on
 * trades that never happened.
 *
 * @param ticker        instrument
 * @param signalAt      open time of the bar the signal was computed from
 * @param entryAt       open time of the bar the position was entered on
 * @param exitAt        open time of the bar the position was exited on
 * @param trigger       planned entry, bar i+1's open, before slippage
 * @param stop          planned stop, the signal bar's low
 * @param target        planned first target, 2R above the trigger
 * @param shares        size from {@link com.moneyteam.trading.service.PositionSizer}
 * @param entryFill     price actually paid, after slippage
 * @param exitFill      price actually received, after slippage
 * @param plannedRisk   {@code trigger − stop}, the denominator for R
 * @param realisedPerShare  {@code exitFill − entryFill − round-trip commission}
 * @param rMultiple     {@code realisedPerShare ÷ plannedRisk}
 * @param outcome       why the position closed
 */
public record BacktestTrade(
        String ticker,
        Instant signalAt,
        Instant entryAt,
        Instant exitAt,
        BigDecimal trigger,
        BigDecimal stop,
        BigDecimal target,
        long shares,
        BigDecimal entryFill,
        BigDecimal exitFill,
        BigDecimal plannedRisk,
        BigDecimal realisedPerShare,
        BigDecimal rMultiple,
        Outcome outcome) {

    /** Why a position closed. */
    public enum Outcome {
        /** Target reached. */
        TARGET,
        /** Stop reached. */
        STOP,
        /**
         * Both the stop and the target were inside one bar's range. OHLC does
         * not record which came first, so the stop is taken and the trade is
         * scored as a loss. Recorded distinctly from {@link #STOP} so the share
         * of results resting on this assumption is visible rather than hidden -
         * if most trades land here, the timeframe is too coarse to draw a
         * conclusion from.
         */
        STOP_AMBIGUOUS_BAR,
        /** Data ran out with the position still open; scored at the last close. */
        END_OF_DATA
    }

    public boolean isWin() {
        return rMultiple.signum() > 0;
    }

    /** True when the outcome rests on the intrabar stop-first assumption. */
    public boolean isAmbiguous() {
        return outcome == Outcome.STOP_AMBIGUOUS_BAR;
    }
}
