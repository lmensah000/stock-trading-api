package com.moneyteam.bot.broker;

import com.moneyteam.trading.model.enums.OrderSide;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A broker that exists entirely in memory.
 *
 * Two jobs. It is the test double for everything above {@link BrokerPort}, and
 * it is what runs when no broker is configured - so a fresh checkout boots and
 * the bot can be exercised end to end without an account anywhere.
 *
 * Fills are immediate and at the intended price, which makes it useless for
 * judging a strategy and ideal for testing plumbing. Anything that needs
 * realistic fills belongs in the backtest, where {@code CostModel} applies
 * slippage deliberately, or against Alpaca's paper host.
 *
 * Thread-safe, because {@link BrokerPort} requires it.
 */
public class InMemoryBroker implements BrokerPort {

    private final Map<String, BrokerOrder> ordersById = new ConcurrentHashMap<>();
    private final Map<String, BrokerOrder> ordersByClientId = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private final Clock clock;

    private volatile boolean marketOpen = true;
    private volatile BigDecimal equity;

    public InMemoryBroker(Clock clock, BigDecimal startingEquity) {
        this.clock = clock;
        this.equity = startingEquity;
    }

    @Override
    public BrokerOrder place(String clientOrderId,
                             String ticker,
                             OrderSide side,
                             BigDecimal quantity,
                             BigDecimal intendedPrice) {

        // Honours the client order id as an idempotency key, like a real broker:
        // a resubmission returns the original rather than creating a second
        // order. Tests of the recovery path need this to behave correctly.
        BrokerOrder existing = ordersByClientId.get(clientOrderId);
        if (existing != null) {
            return existing;
        }

        BrokerOrder order = new BrokerOrder(
                "mem-" + sequence.incrementAndGet(),
                clientOrderId,
                ticker.toUpperCase(),
                side,
                quantity,
                quantity,
                intendedPrice,
                intendedPrice,
                BrokerOrder.Status.FILLED,
                clock.instant());

        ordersById.put(order.brokerOrderId(), order);
        ordersByClientId.put(clientOrderId, order);
        return order;
    }

    @Override
    public Optional<BrokerOrder> getOrder(String brokerOrderId) {
        return Optional.ofNullable(ordersById.get(brokerOrderId));
    }

    @Override
    public void cancel(String brokerOrderId) {
        // Everything fills immediately here, so there is nothing cancellable.
        // Silent, because BrokerPort specifies that cancelling a terminal order
        // succeeds.
    }

    @Override
    public List<BrokerPosition> getPositions() {
        return List.of();
    }

    @Override
    public BrokerAccount getAccount() {
        return new BrokerAccount("in-memory", equity, equity, equity, true);
    }

    @Override
    public boolean isMarketOpen() {
        return marketOpen;
    }

    @Override
    public String describe() {
        return "InMemoryBroker[PAPER, fills immediately at the intended price]";
    }

    /** Test hook: simulate a closed market. */
    public InMemoryBroker setMarketOpen(boolean open) {
        this.marketOpen = open;
        return this;
    }

    /** Every order placed, in no particular order. */
    public List<BrokerOrder> placedOrders() {
        return new ArrayList<>(ordersById.values());
    }
}
