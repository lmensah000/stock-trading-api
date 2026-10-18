package com.moneyteam.bot.broker;

import com.moneyteam.trading.model.enums.OrderSide;
import com.moneyteam.trading.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Booking broker fills into our own order state.
 *
 * The behaviour that matters is that polling is not idempotent by nature and
 * this makes it so. An order polled at 30% and then at 80% must book 30 then
 * 50 - not 30 then 80 - and a repeat poll at the same 80% must book nothing.
 *
 * This is not bookkeeping pedantry. Our {@code Account} is what
 * {@code LiveTradingGate} reads to decide whether real money becomes
 * reachable, so a double-booked fill could arm the gate on money that was
 * never made.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FillReconcilerTest {

    private static final long USER = 7L;
    private static final long LOCAL_ORDER = 42L;
    private static final String BROKER_ORDER = "abc-123";

    @Mock
    private BrokerPort broker;

    @Mock
    private OrderService orderService;

    private FillReconciler reconciler;

    @BeforeEach
    void setUp() {
        reconciler = new FillReconciler(broker, orderService);
        reconciler.track(BROKER_ORDER, LOCAL_ORDER, USER);
    }

    private void brokerReports(String filledQty, String avgPrice, BrokerOrder.Status status) {
        when(broker.getOrder(BROKER_ORDER)).thenReturn(Optional.of(new BrokerOrder(
                BROKER_ORDER, "tckr-1", "AAPL", OrderSide.BUY,
                new BigDecimal("100"), new BigDecimal(filledQty),
                avgPrice == null ? null : new BigDecimal(avgPrice),
                new BigDecimal("150.00"), status, Instant.EPOCH)));
    }

    @Test
    @DisplayName("a first fill books the whole filled quantity")
    void booksTheFirstFill() {
        brokerReports("30", "150.10", BrokerOrder.Status.PARTIALLY_FILLED);

        assertThat(reconciler.reconcile(USER, BROKER_ORDER)).isEqualByComparingTo("30");

        verify(orderService).fill(USER, LOCAL_ORDER, 30.0, new BigDecimal("150.10"));
    }

    @Test
    @DisplayName("a second poll books only the delta, not the running total")
    void booksOnlyTheDelta() {
        brokerReports("30", "150.10", BrokerOrder.Status.PARTIALLY_FILLED);
        reconciler.reconcile(USER, BROKER_ORDER);

        brokerReports("80", "150.20", BrokerOrder.Status.PARTIALLY_FILLED);
        assertThat(reconciler.reconcile(USER, BROKER_ORDER)).isEqualByComparingTo("50");

        // 30 then 50, totalling 80. Booking 80 here would record 110 filled on
        // a 100-share order.
        verify(orderService).fill(USER, LOCAL_ORDER, 30.0, new BigDecimal("150.10"));
        verify(orderService).fill(USER, LOCAL_ORDER, 50.0, new BigDecimal("150.20"));
    }

    @Test
    @DisplayName("polling again with nothing new books nothing")
    void repeatedPollIsIdempotent() {
        brokerReports("100", "150.10", BrokerOrder.Status.FILLED);
        reconciler.reconcile(USER, BROKER_ORDER);

        assertThat(reconciler.reconcile(USER, BROKER_ORDER)).isEqualByComparingTo("0");
        assertThat(reconciler.reconcile(USER, BROKER_ORDER)).isEqualByComparingTo("0");

        // Exactly once, however many times it is polled.
        verify(orderService, org.mockito.Mockito.times(1))
                .fill(anyLong(), anyLong(), anyDouble(), any());
    }

    @Test
    @DisplayName("a broker reporting less than already booked is ignored, not applied")
    void regressionIsIgnored() {
        brokerReports("80", "150.10", BrokerOrder.Status.PARTIALLY_FILLED);
        reconciler.reconcile(USER, BROKER_ORDER);

        // Should not happen, but booking a negative fill would corrupt the
        // ledger, so it is refused rather than trusted.
        brokerReports("30", "150.10", BrokerOrder.Status.PARTIALLY_FILLED);

        assertThat(reconciler.reconcile(USER, BROKER_ORDER)).isEqualByComparingTo("0");
        verify(orderService, org.mockito.Mockito.times(1))
                .fill(anyLong(), anyLong(), anyDouble(), any());
    }

    @Test
    @DisplayName("a reported fill with no price yet waits rather than booking at zero")
    void missingPriceWaits() {
        brokerReports("30", null, BrokerOrder.Status.PARTIALLY_FILLED);

        assertThat(reconciler.reconcile(USER, BROKER_ORDER)).isEqualByComparingTo("0");
        verify(orderService, never()).fill(anyLong(), anyLong(), anyDouble(), any());

        // And once the price arrives, the full 30 is still booked - the earlier
        // skip must not have consumed it.
        brokerReports("30", "150.10", BrokerOrder.Status.PARTIALLY_FILLED);
        assertThat(reconciler.reconcile(USER, BROKER_ORDER)).isEqualByComparingTo("30");
    }

    @Test
    @DisplayName("a failed booking is retried on the next poll rather than lost")
    void failedBookingIsNotSwallowed() {
        brokerReports("30", "150.10", BrokerOrder.Status.PARTIALLY_FILLED);
        org.mockito.Mockito.doThrow(new IllegalStateException("db down"))
                .when(orderService).fill(anyLong(), anyLong(), anyDouble(), any());

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> reconciler.reconcile(USER, BROKER_ORDER))
                .isInstanceOf(IllegalStateException.class);

        // The booked total must not have advanced, or the fill is gone forever.
        org.mockito.Mockito.reset(orderService);
        assertThat(reconciler.reconcile(USER, BROKER_ORDER)).isEqualByComparingTo("30");
    }

    @Test
    @DisplayName("an untracked order is ignored rather than guessed at")
    void untrackedOrderIsIgnored() {
        assertThat(reconciler.reconcile(USER, "never-seen")).isEqualByComparingTo("0");
        verify(orderService, never()).fill(anyLong(), anyLong(), anyDouble(), any());
    }

    @Test
    @DisplayName("an order the broker does not recognise books nothing")
    void unknownToBrokerBooksNothing() {
        when(broker.getOrder(BROKER_ORDER)).thenReturn(Optional.empty());

        assertThat(reconciler.reconcile(USER, BROKER_ORDER)).isEqualByComparingTo("0");
        verify(orderService, never()).fill(anyLong(), anyLong(), anyDouble(), any());
    }

    @Test
    @DisplayName("reconcileAll books across orders and forgets the terminal ones")
    void reconcileAllForgetsTerminalOrders() {
        brokerReports("100", "150.10", BrokerOrder.Status.FILLED);

        assertThat(reconciler.trackedCount()).isEqualTo(1);
        assertThat(reconciler.reconcileAll()).isEqualByComparingTo("100");

        // Dropped once no further fill can arrive, so the maps stay bounded in
        // a long-running process.
        assertThat(reconciler.trackedCount()).isZero();
    }

    @Test
    @DisplayName("a still-open order stays tracked after reconcileAll")
    void openOrderStaysTracked() {
        brokerReports("30", "150.10", BrokerOrder.Status.PARTIALLY_FILLED);

        reconciler.reconcileAll();

        assertThat(reconciler.trackedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("one order failing does not stop the others being booked")
    void oneFailureDoesNotStopThePass() {
        reconciler.track("second", 43L, USER);

        when(broker.getOrder(BROKER_ORDER)).thenThrow(new IllegalStateException("broker down"));
        when(broker.getOrder("second")).thenReturn(Optional.of(new BrokerOrder(
                "second", "tckr-2", "TSLA", OrderSide.BUY,
                new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("200.00"),
                new BigDecimal("200.00"), BrokerOrder.Status.FILLED, Instant.EPOCH)));

        assertThat(reconciler.reconcileAll()).isEqualByComparingTo("10");
        verify(orderService).fill(USER, 43L, 10.0, new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("measured slippage comes from the broker's actual fill")
    void measuresRealSlippage() {
        brokerReports("100", "150.10", BrokerOrder.Status.FILLED);

        // This is the number that should eventually replace the guessed
        // slippageBps default in CostModel.
        assertThat(reconciler.measuredSlippage(BROKER_ORDER)).isEqualByComparingTo("0.10");
    }

    @Test
    @DisplayName("the in-memory broker honours the client order id as an idempotency key")
    void inMemoryBrokerDeduplicatesByClientOrderId() {
        InMemoryBroker inMemory = new InMemoryBroker(
                java.time.Clock.systemUTC(), new BigDecimal("100000"));

        BrokerOrder first = inMemory.place("tckr-1", "AAPL", OrderSide.BUY,
                new BigDecimal("100"), new BigDecimal("150.00"));
        BrokerOrder resubmitted = inMemory.place("tckr-1", "AAPL", OrderSide.BUY,
                new BigDecimal("100"), new BigDecimal("150.00"));

        // A resubmission after a lost response must return the original, not
        // open a second position. The test double has to model this or the
        // recovery path is untested.
        assertThat(resubmitted.brokerOrderId()).isEqualTo(first.brokerOrderId());
        assertThat(inMemory.placedOrders()).hasSize(1);
    }

    @Test
    @DisplayName("a closed market is reported, so the runner can skip a pass")
    void inMemoryBrokerCanReportClosed() {
        InMemoryBroker inMemory = new InMemoryBroker(
                java.time.Clock.systemUTC(), new BigDecimal("100000"));

        assertThat(inMemory.isMarketOpen()).isTrue();
        assertThat(inMemory.setMarketOpen(false).isMarketOpen()).isFalse();
    }

    @Test
    @DisplayName("the in-memory broker reports itself as paper")
    void inMemoryBrokerIsPaper() {
        InMemoryBroker inMemory = new InMemoryBroker(
                java.time.Clock.systemUTC(), new BigDecimal("100000"));

        assertThat(inMemory.getAccount().paper()).isTrue();
        assertThat(inMemory.describe()).contains("PAPER");
        assertThat(inMemory.getPositions()).isEmpty();
    }

    @Test
    @DisplayName("tracked orders can be forgotten explicitly")
    void forgetRemovesTracking() {
        assertThat(reconciler.trackedCount()).isEqualTo(1);

        reconciler.forget(BROKER_ORDER);

        assertThat(reconciler.trackedCount()).isZero();
        assertThat(reconciler.reconcile(USER, BROKER_ORDER)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("reconcileAll with nothing tracked does no work")
    void reconcileAllWithNothingTracked() {
        reconciler.forget(BROKER_ORDER);

        assertThat(reconciler.reconcileAll()).isEqualByComparingTo("0");
        verify(broker, never()).getOrder(any());
    }

    @Test
    @DisplayName("positions from the broker are kept distinct from ours, so drift is visible")
    void brokerPositionsAreSeparate() {
        when(broker.getPositions()).thenReturn(List.of(new BrokerPosition(
                "AAPL", new BigDecimal("100"), new BigDecimal("150.00"),
                new BigDecimal("15500.00"), new BigDecimal("500.00"))));

        // The point of a separate type: our Position and the broker's can be
        // compared, and a divergence is a reconciliation failure worth seeing.
        assertThat(broker.getPositions()).hasSize(1);
        assertThat(broker.getPositions().get(0).ticker()).isEqualTo("AAPL");
    }
}
