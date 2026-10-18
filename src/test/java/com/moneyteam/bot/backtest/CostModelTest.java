package com.moneyteam.bot.backtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Costs, and the structural guarantee that a backtest cannot write to the
 * database.
 */
class CostModelTest {

    @Test
    @DisplayName("slippage moves the fill against the position on both sides")
    void slippageIsAdverseInBothDirections() {
        CostModel tenBps = new CostModel(new BigDecimal("10"), BigDecimal.ZERO);

        // A long pays more than it wanted and receives less than it wanted.
        // Charging it one way only halves the real cost.
        assertThat(tenBps.buyFill(new BigDecimal("100.00"))).isEqualByComparingTo("100.1000");
        assertThat(tenBps.sellFill(new BigDecimal("100.00"))).isEqualByComparingTo("99.9000");
    }

    @Test
    @DisplayName("the spec's worked fills reproduce exactly")
    void matchesSpecFills() {
        CostModel tenBps = new CostModel(new BigDecimal("10"), BigDecimal.ZERO);

        assertThat(tenBps.buyFill(new BigDecimal("105.50"))).isEqualByComparingTo("105.6055");
        assertThat(tenBps.sellFill(new BigDecimal("117.50"))).isEqualByComparingTo("117.3825");
    }

    @Test
    @DisplayName("commission is charged on both entry and exit")
    void commissionIsRoundTrip() {
        CostModel model = new CostModel(BigDecimal.ZERO, new BigDecimal("0.005"));

        assertThat(model.roundTripCommissionPerShare()).isEqualByComparingTo("0.0100");
    }

    @Test
    @DisplayName("the free model changes nothing")
    void freeModelIsNeutral() {
        CostModel free = CostModel.free();

        assertThat(free.buyFill(new BigDecimal("100.00"))).isEqualByComparingTo("100.00");
        assertThat(free.sellFill(new BigDecimal("100.00"))).isEqualByComparingTo("100.00");
        assertThat(free.roundTripCommissionPerShare()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("negative costs are refused, since they would be a free lunch")
    void negativeCostsAreRefused() {
        assertThatThrownBy(() -> new CostModel(new BigDecimal("-1"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("slippageBps");

        assertThatThrownBy(() -> new CostModel(BigDecimal.ZERO, new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("commissionPerShare");
    }

    @Test
    @DisplayName("the runner holds no repository, so a replay cannot write to the database")
    void runnerCannotReachThePersistenceLayer() {
        // Structural rather than behavioural on purpose: a test that merely
        // observed no writes during one run would pass while leaving the
        // capability in place. A backtest that booked simulated fills into
        // Account would feed the live-trading gate, which reads equity to
        // decide whether real money becomes reachable.
        String fields = Arrays.stream(BacktestRunner.class.getDeclaredFields())
                .map(Field::getType)
                .map(Class::getName)
                .reduce("", (a, b) -> a + " " + b);

        assertThat(fields)
                .doesNotContain("Repository")
                .doesNotContain("EntityManager")
                .doesNotContain("Service");
    }

    @Test
    @DisplayName("a backtest trade is a record, not a JPA entity")
    void tradeIsNotPersistable() {
        assertThat(BacktestTrade.class.isRecord()).isTrue();
        assertThat(BacktestTrade.class.getAnnotations()).isEmpty();
    }
}
