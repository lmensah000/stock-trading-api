package com.moneyteam.bot.broker;

import com.moneyteam.trading.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brings the broker's fills into our own order state.
 *
 * <h2>One source of truth</h2>
 * The broker is authoritative for fills; we record them. Two systems each
 * deciding what filled is the classic source of divergence here, and the
 * divergence is not benign - our {@code Account} feeds
 * {@code LiveTradingGate}, so a double-booked fill could arm the gate on money
 * that was never made.
 *
 * <h2>Why it tracks what it has already booked</h2>
 * Polling is not idempotent by nature. An order polled at 30% filled and again
 * at 80% must book 30 then 50, not 30 then 80, and a repeat poll at the same
 * 80% must book nothing. So this keeps the quantity already recorded per order
 * and books only the delta.
 *
 * That state is in memory, which is a deliberate limit worth stating: after a
 * restart an order already partly booked would be re-booked from zero. The fix
 * is a column on {@code Order} recording broker-filled quantity, which belongs
 * with the live adapter. Until then, run reconciliation within one process
 * lifetime - acceptable while this is paper, not acceptable for live.
 */
public class FillReconciler {

    private static final Logger log = LoggerFactory.getLogger(FillReconciler.class);

    private final BrokerPort broker;
    private final OrderService orderService;

    /** Broker order id to the quantity already booked into our order. */
    private final Map<String, BigDecimal> bookedQuantity = new ConcurrentHashMap<>();

    /** Broker order id to the local order and the user it belongs to. */
    private final Map<String, Tracked> tracked = new ConcurrentHashMap<>();

    private record Tracked(Long localOrderId, Long userId) {
    }

    public FillReconciler(BrokerPort broker, OrderService orderService) {
        this.broker = broker;
        this.orderService = orderService;
    }

    /**
     * Associates a broker order with the local order it was placed for.
     *
     * The user id is stored alongside rather than looked up later, because
     * {@code OrderService} takes it as an explicit parameter and the
     * reconciler runs on a scheduler thread with no authenticated principal.
     */
    public void track(String brokerOrderId, Long localOrderId, Long userId) {
        tracked.put(brokerOrderId, new Tracked(localOrderId, userId));
        bookedQuantity.putIfAbsent(brokerOrderId, BigDecimal.ZERO);
    }

    /**
     * Polls every tracked order, books what is new, and stops tracking the ones
     * that can take no further fills.
     *
     * @return total quantity booked across all orders in this pass
     */
    public BigDecimal reconcileAll() {
        BigDecimal booked = BigDecimal.ZERO;

        // Copied so an order forgotten mid-pass does not disturb the iteration.
        for (String brokerOrderId : List.copyOf(tracked.keySet())) {
            Tracked entry = tracked.get(brokerOrderId);
            if (entry == null) {
                continue;
            }
            try {
                booked = booked.add(reconcile(entry.userId(), brokerOrderId));

                // Terminal orders are dropped so the maps stay bounded over a
                // long-running process.
                broker.getOrder(brokerOrderId)
                        .filter(BrokerOrder::isTerminal)
                        .ifPresent(order -> forget(brokerOrderId));
            } catch (RuntimeException e) {
                // One order's failure must not stop the others being booked.
                log.warn("Could not reconcile broker order {}: {}", brokerOrderId, e.toString());
            }
        }
        return booked;
    }

    /**
     * Polls one order and books whatever is newly filled.
     *
     * @return the quantity booked by this call, which is zero when nothing
     *         changed since the last poll
     */
    public BigDecimal reconcile(Long userId, String brokerOrderId) {
        Tracked entry = tracked.get(brokerOrderId);
        if (entry == null) {
            log.warn("Asked to reconcile broker order {} which was never tracked; ignoring",
                    brokerOrderId);
            return BigDecimal.ZERO;
        }

        Optional<BrokerOrder> found = broker.getOrder(brokerOrderId);
        if (found.isEmpty()) {
            log.warn("Broker does not recognise order {}", brokerOrderId);
            return BigDecimal.ZERO;
        }

        BrokerOrder order = found.get();
        BigDecimal alreadyBooked = bookedQuantity.getOrDefault(brokerOrderId, BigDecimal.ZERO);
        BigDecimal filled = order.filledQuantity() == null ? BigDecimal.ZERO : order.filledQuantity();
        BigDecimal delta = filled.subtract(alreadyBooked);

        if (delta.signum() <= 0) {
            // Nothing new, or the broker reported less than before - which
            // should not happen, but booking a negative fill would corrupt the
            // ledger, so it is logged rather than applied.
            if (delta.signum() < 0) {
                log.warn("Broker order {} reported {} filled, below the {} already booked. "
                        + "Ignoring; this needs investigating.", brokerOrderId, filled, alreadyBooked);
            }
            return BigDecimal.ZERO;
        }

        BigDecimal price = order.averageFillPrice();
        if (price == null) {
            log.warn("Broker order {} reports {} filled but no average price yet; waiting",
                    brokerOrderId, filled);
            return BigDecimal.ZERO;
        }

        // Booked before the local record is updated: if fill() throws, the
        // delta is retried on the next poll rather than lost. Booking the
        // quantity first would silently drop a fill.
        orderService.fill(userId, entry.localOrderId(), delta.doubleValue(), price);
        bookedQuantity.put(brokerOrderId, filled);

        BigDecimal slippage = order.adverseSlippage();
        log.info("Booked {} of {} on order {} at {}{}",
                delta, order.ticker(), brokerOrderId, price,
                slippage == null ? "" : " (slippage " + slippage.setScale(4, RoundingMode.HALF_UP) + ")");

        return delta;
    }

    /**
     * Measured adverse slippage for a tracked order, or null when unknown.
     *
     * This is the number that should eventually replace the guessed
     * {@code slippageBps} default in {@code CostModel}: real slippage from real
     * order flow, rather than a figure chosen because it sounded plausible.
     */
    public BigDecimal measuredSlippage(String brokerOrderId) {
        return broker.getOrder(brokerOrderId).map(BrokerOrder::adverseSlippage).orElse(null);
    }

    /** Stops tracking a terminal order, so the maps do not grow without bound. */
    public void forget(String brokerOrderId) {
        bookedQuantity.remove(brokerOrderId);
        tracked.remove(brokerOrderId);
    }

    /** Broker order ids currently tracked. */
    public int trackedCount() {
        return tracked.size();
    }
}
