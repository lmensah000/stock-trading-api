package com.moneyteam.bot.broker;

import com.moneyteam.trading.model.enums.OrderSide;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * The port through which the bot reaches a broker.
 *
 * One interface, several implementations: {@code InMemoryBroker} offline and in
 * tests, {@code AlpacaBroker} against Alpaca's paper host, and a live adapter
 * later. The point of the seam is that adding live execution is a new
 * implementation rather than a change to anything above it - and that the
 * decision about which one is in use sits in configuration, where it can be
 * audited, rather than scattered through the call sites.
 *
 * Implementations must be thread-safe. Nothing above this interface should
 * assume a call is cheap: every method may be a network round trip under a rate
 * limit.
 */
public interface BrokerPort {

    /**
     * Submits an order.
     *
     * @param clientOrderId an id <em>we</em> generate, which the broker echoes
     *        back. This is the idempotency key: if the response is lost to a
     *        timeout, the order may still have been accepted, and resubmitting
     *        blindly would double the position. With a client id the same
     *        submission can be recognised instead of repeated.
     * @param intendedPrice the price the plan assumed. Carried through so the
     *        actual fill can be compared against it and real slippage measured
     *        rather than guessed.
     */
    BrokerOrder place(String clientOrderId,
                      String ticker,
                      OrderSide side,
                      BigDecimal quantity,
                      BigDecimal intendedPrice);

    /** The broker's current view of an order, or empty if it does not know it. */
    Optional<BrokerOrder> getOrder(String brokerOrderId);

    /** Cancels an order. Succeeds silently if it was already terminal. */
    void cancel(String brokerOrderId);

    /** Open positions as the broker sees them. */
    List<BrokerPosition> getPositions();

    /** Account snapshot as the broker sees it. */
    BrokerAccount getAccount();

    /**
     * Whether the market is open for trading right now.
     *
     * Asked before a scan so a closed market costs one cheap call rather than a
     * run of rejected orders and a spent rate limit.
     */
    boolean isMarketOpen();

    /**
     * A short description of what this implementation talks to, for logging.
     * Must make the paper/live distinction obvious at a glance - a log line that
     * does not say which account was traded is worthless after the fact.
     */
    String describe();
}
