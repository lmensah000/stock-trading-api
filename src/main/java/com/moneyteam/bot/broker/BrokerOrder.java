package com.moneyteam.bot.broker;

import com.moneyteam.trading.model.enums.OrderSide;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * An order as the broker sees it.
 *
 * Deliberately separate from {@code trading.model.Order}. That one is a JPA
 * entity carrying our own state machine; this is a wire-level view of someone
 * else's record. Collapsing them would mean a broker's status vocabulary
 * silently became our persisted state, and the two do not line up - Alpaca has
 * {@code accepted}, {@code pending_new} and {@code held} with no counterpart
 * in {@code OrderStatus}.
 *
 * @param brokerOrderId the broker's id, used to poll this order later
 * @param clientOrderId our id, echoed back by the broker; see
 *                      {@link BrokerPort#place} for why it matters
 * @param ticker        instrument
 * @param side          BUY or SELL
 * @param quantity      shares requested
 * @param filledQuantity shares filled so far
 * @param averageFillPrice volume-weighted average fill, or null before any fill
 * @param intendedPrice the price the plan expected, for slippage measurement
 * @param status        normalised status
 * @param submittedAt   when the broker accepted it
 */
public record BrokerOrder(
        String brokerOrderId,
        String clientOrderId,
        String ticker,
        OrderSide side,
        BigDecimal quantity,
        BigDecimal filledQuantity,
        BigDecimal averageFillPrice,
        BigDecimal intendedPrice,
        Status status,
        Instant submittedAt) {

    /**
     * Broker statuses, normalised.
     *
     * Narrower than Alpaca's vocabulary on purpose: every value a broker can
     * report maps onto one of these, and the mapping lives in the adapter. The
     * rest of the application should not have to learn one broker's words.
     */
    public enum Status {
        /** Accepted, nothing filled. */
        OPEN,
        /** Some but not all of the quantity filled. */
        PARTIALLY_FILLED,
        /** Fully filled. */
        FILLED,
        /** Cancelled before completion. */
        CANCELLED,
        /** Refused by the broker, or expired. */
        REJECTED
    }

    /** True once no further fills can arrive. */
    public boolean isTerminal() {
        return status == Status.FILLED || status == Status.CANCELLED || status == Status.REJECTED;
    }

    /**
     * Fill price minus the price the plan assumed, signed so that a positive
     * number always means it went against us.
     *
     * A long filled above its intended price and a short filled below it are
     * both adverse, so the sign is flipped for a SELL. Reporting raw difference
     * would make half the measurements look like gains.
     */
    public BigDecimal adverseSlippage() {
        if (averageFillPrice == null || intendedPrice == null) {
            return null;
        }
        BigDecimal raw = averageFillPrice.subtract(intendedPrice);
        return side == OrderSide.BUY ? raw : raw.negate();
    }
}
