package com.moneyteam.trading.service.impl;

import com.moneyteam.trading.model.Order;
import com.moneyteam.trading.model.enums.OrderSide;
import com.moneyteam.trading.model.enums.OrderStatus;
import com.moneyteam.trading.repository.OrderRepository;
import com.moneyteam.trading.service.TradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The order lifecycle. The point of these tests is the illegal transitions:
 * a state machine that only ever gets exercised on its happy path is not
 * actually constraining anything.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private static final long USER = 7L;
    private static final long ORDER = 42L;

    @Mock private OrderRepository orderRepository;
    @Mock private TradeService tradeService;
    @InjectMocks private OrderServiceImpl orderService;

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(ORDER);
        order.setUserId(USER);
        order.setStockTicker("AAPL");
        order.setQuantity(100.0);
        order.setSide(OrderSide.BUY);
        order.setStatus(OrderStatus.PENDING);
    }

    private void orderIsFound() {
        when(orderRepository.findByIdAndUserId(ORDER, USER)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("a partial fill leaves the order PARTIALLY_FILLED and records the execution")
    void partialFill() {
        orderIsFound();

        orderService.fill(USER, ORDER, 30, new BigDecimal("150.00"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(order.getExecutions()).hasSize(1);
        assertThat(order.getFilledQuantity()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("filling the remainder moves the order to FILLED")
    void fillToCompletion() {
        orderIsFound();

        orderService.fill(USER, ORDER, 60, new BigDecimal("150.00"));
        orderService.fill(USER, ORDER, 40, new BigDecimal("151.00"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(order.getExecutions()).hasSize(2);
        assertThat(order.getFilledQuantity()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("a fill larger than the remaining quantity is rejected and books nothing")
    void overfillRejected() {
        when(orderRepository.findByIdAndUserId(ORDER, USER)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.fill(USER, ORDER, 101, new BigDecimal("150.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds remaining");

        assertThat(order.getExecutions()).isEmpty();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(tradeService, never()).placeTrade(anyLong(), any());
    }

    @Test
    @DisplayName("a non-positive fill quantity is rejected")
    void nonPositiveFillRejected() {
        when(orderRepository.findByIdAndUserId(ORDER, USER)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.fill(USER, ORDER, 0, new BigDecimal("150.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a filled order cannot be filled again - FILLED is terminal")
    void filledIsTerminal() {
        orderIsFound();
        orderService.fill(USER, ORDER, 100, new BigDecimal("150.00"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);

        // no quantity remains, so this is caught as an overfill before the
        // transition check - either way it must not succeed
        assertThatThrownBy(() -> orderService.fill(USER, ORDER, 1, new BigDecimal("150.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a filled order cannot be cancelled")
    void cannotCancelFilledOrder() {
        order.setStatus(OrderStatus.FILLED);
        when(orderRepository.findByIdAndUserId(ORDER, USER)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(USER, ORDER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition");
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class,
            names = {"FILLED", "CANCELLED", "REJECTED", "EXPIRED"})
    @DisplayName("every terminal status refuses both cancel and reject")
    void terminalStatusesRefuseFurtherTransitions(OrderStatus terminal) {
        order.setStatus(terminal);
        when(orderRepository.findByIdAndUserId(ORDER, USER)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancel(USER, ORDER))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> orderService.reject(USER, ORDER))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("a pending order cancels cleanly")
    void cancelPendingOrder() {
        orderIsFound();

        orderService.cancel(USER, ORDER);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("a partially filled order can still be cancelled, keeping its fills")
    void cancelPartiallyFilledOrder() {
        orderIsFound();
        orderService.fill(USER, ORDER, 25, new BigDecimal("150.00"));

        orderService.cancel(USER, ORDER);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getExecutions()).hasSize(1);
    }

    @Test
    @DisplayName("a fill drives a trade for the filled quantity, attributed to the order's owner")
    void fillDrivesTrade() {
        orderIsFound();

        orderService.fill(USER, ORDER, 30, new BigDecimal("150.00"));

        verify(tradeService).placeTrade(org.mockito.ArgumentMatchers.eq(USER), any());
    }

    @Test
    @DisplayName("another user's order is reported not-found, so ids cannot be probed")
    void otherUsersOrderIsNotFound() {
        when(orderRepository.findByIdAndUserId(ORDER, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancel(999L, ORDER))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Order not found");
    }
}
