package com.moneyteam.trading.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Average-cost P&L, hand-computed.
 *
 * Values are compared with isEqualByComparingTo rather than isEqualTo because
 * BigDecimal.equals is scale-sensitive (2.0 != 2.00), and scale is asserted
 * separately where it is part of the contract.
 */
class PnLCalculatorTest {

    private static BigDecimal money(String v) {
        return new BigDecimal(v);
    }

    @Nested
    @DisplayName("realizedPnL")
    class RealizedPnL {

        @Test
        @DisplayName("books a gain when the exit is above average cost")
        void gain() {
            // bought at 100, sold 10 at 110 -> (110 - 100) * 10
            assertThat(PnLCalculator.realizedPnL(money("100.00"), money("110.00"), 10))
                    .isEqualByComparingTo("100.0000");
        }

        @Test
        @DisplayName("books a loss when the exit is below average cost")
        void loss() {
            assertThat(PnLCalculator.realizedPnL(money("100.00"), money("90.00"), 10))
                    .isEqualByComparingTo("-100.0000");
        }

        @Test
        @DisplayName("books zero when the exit equals average cost")
        void flat() {
            assertThat(PnLCalculator.realizedPnL(money("100.00"), money("100.00"), 10))
                    .isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("scales a partial exit by the quantity sold, not the position size")
        void partialExit() {
            // holding is irrelevant: only the 3 shares sold are realized
            assertThat(PnLCalculator.realizedPnL(money("50.00"), money("57.50"), 3))
                    .isEqualByComparingTo("22.5000");
        }

        @Test
        @DisplayName("always returns scale 4, so it lands cleanly in DECIMAL(19,4)")
        void scaleIsFixed() {
            assertThat(PnLCalculator.realizedPnL(money("10"), money("11"), 1).scale()).isEqualTo(4);
        }

        @Test
        @DisplayName("rounds half up at the fourth decimal place")
        void roundsHalfUp() {
            // (0.00005 difference) * 1 share = 0.00005 -> 0.0001 at scale 4
            assertThat(PnLCalculator.realizedPnL(money("1.00000"), money("1.00005"), 1))
                    .isEqualByComparingTo("0.0001");
        }

        @Test
        @DisplayName("handles fractional quantities without losing precision")
        void fractionalQuantity() {
            assertThat(PnLCalculator.realizedPnL(money("100.00"), money("110.00"), 2.5))
                    .isEqualByComparingTo("25.0000");
        }
    }

    @Nested
    @DisplayName("unrealizedPnL")
    class UnrealizedPnL {

        @Test
        @DisplayName("is positive when the mark is above average cost")
        void markedUp() {
            assertThat(PnLCalculator.unrealizedPnL(money("100.00"), money("110.00"), 10))
                    .isEqualByComparingTo("100");
        }

        @Test
        @DisplayName("is negative when the mark is below average cost")
        void markedDown() {
            assertThat(PnLCalculator.unrealizedPnL(money("100.00"), money("85.50"), 4))
                    .isEqualByComparingTo("-58");
        }

        @Test
        @DisplayName("is zero for a flat position regardless of price")
        void zeroQuantity() {
            assertThat(PnLCalculator.unrealizedPnL(money("100.00"), money("250.00"), 0))
                    .isEqualByComparingTo("0");
        }

        /**
         * Documents current behaviour rather than endorsing it: unrealizedPnL does
         * not pin a scale the way realizedPnL does, so its scale follows from the
         * inputs. Persisting to DECIMAL(19,4) rounds it anyway, but the asymmetry
         * between the two methods is raised in the pull request rather than being
         * changed inside a test-only commit.
         */
        @Test
        @DisplayName("does not pin its own scale (asymmetry with realizedPnL)")
        void scaleIsNotPinned() {
            BigDecimal result = PnLCalculator.unrealizedPnL(money("100.00"), money("110.00"), 10);
            assertThat(result.scale()).isNotEqualTo(4);
        }
    }
}
