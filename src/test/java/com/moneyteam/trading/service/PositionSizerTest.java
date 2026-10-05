package com.moneyteam.trading.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Risk-based sizing.
 *
 * The behaviour worth protecting is that a wider stop produces a smaller
 * position automatically, so risk per trade stays flat regardless of how
 * volatile the instrument is.
 */
class PositionSizerTest {

    private static final BigDecimal EQUITY = new BigDecimal("100000");
    private static final BigDecimal ONE_PERCENT = BigDecimal.ONE;

    @Test
    @DisplayName("risks exactly the configured percentage of equity")
    void sizesToRiskBudget() {
        // 1% of 100,000 = 1,000 risk budget; 2.00 per share of risk -> 500 shares
        long shares = PositionSizer.sharesForRisk(
                EQUITY, ONE_PERCENT, new BigDecimal("50.00"), new BigDecimal("48.00"));

        assertThat(shares).isEqualTo(500);
    }

    @Test
    @DisplayName("a wider stop buys fewer shares, with no change to the risk budget")
    void widerStopMeansSmallerPosition() {
        long tight = PositionSizer.sharesForRisk(
                EQUITY, ONE_PERCENT, new BigDecimal("50.00"), new BigDecimal("49.00")); // 1.00 risk
        long wide = PositionSizer.sharesForRisk(
                EQUITY, ONE_PERCENT, new BigDecimal("50.00"), new BigDecimal("45.00")); // 5.00 risk

        assertThat(tight).isEqualTo(1000);
        assertThat(wide).isEqualTo(200);
        assertThat(wide).isLessThan(tight);

        // Both risk the same 1,000 if stopped out - that is the whole point.
        assertThat(new BigDecimal("1.00").multiply(BigDecimal.valueOf(tight)))
                .isEqualByComparingTo(new BigDecimal("5.00").multiply(BigDecimal.valueOf(wide)));
    }

    @Test
    @DisplayName("share count rounds down so the risk budget is never exceeded")
    void roundsDownRatherThanNearest() {
        // 1,000 budget / 3.00 per share = 333.33 -> 333, not 334
        long shares = PositionSizer.sharesForRisk(
                EQUITY, ONE_PERCENT, new BigDecimal("50.00"), new BigDecimal("47.00"));

        assertThat(shares).isEqualTo(333);
    }

    @Test
    @DisplayName("doubling risk percent doubles the position")
    void riskPercentScalesLinearly() {
        long one = PositionSizer.sharesForRisk(
                EQUITY, BigDecimal.ONE, new BigDecimal("50.00"), new BigDecimal("48.00"));
        long two = PositionSizer.sharesForRisk(
                EQUITY, BigDecimal.valueOf(2), new BigDecimal("50.00"), new BigDecimal("48.00"));

        assertThat(two).isEqualTo(one * 2);
    }

    @Test
    @DisplayName("a stop at or above the entry has no definable risk, so no size")
    void stopMustBeBelowTrigger() {
        assertThatThrownBy(() -> PositionSizer.sharesForRisk(
                EQUITY, ONE_PERCENT, new BigDecimal("50.00"), new BigDecimal("50.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be below trigger");

        assertThatThrownBy(() -> PositionSizer.sharesForRisk(
                EQUITY, ONE_PERCENT, new BigDecimal("50.00"), new BigDecimal("52.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("non-positive equity or risk is refused")
    void refusesNonsenseInputs() {
        assertThatThrownBy(() -> PositionSizer.sharesForRisk(
                BigDecimal.ZERO, ONE_PERCENT, new BigDecimal("50"), new BigDecimal("48")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> PositionSizer.sharesForRisk(
                EQUITY, BigDecimal.ZERO, new BigDecimal("50"), new BigDecimal("48")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("the first target sits at a multiple of the risk taken")
    void firstTargetIsAnRMultiple() {
        // risk 2.00 per share; 2R target is 50 + 4 = 54
        assertThat(PositionSizer.firstTarget(
                new BigDecimal("50.00"), new BigDecimal("48.00"), BigDecimal.valueOf(2)))
                .isEqualByComparingTo("54.0000");
    }

    @Test
    @DisplayName("position value is the cash committed, which is not the amount at risk")
    void positionValueIsDistinctFromRisk() {
        long shares = PositionSizer.sharesForRisk(
                EQUITY, ONE_PERCENT, new BigDecimal("50.00"), new BigDecimal("48.00"));

        // 500 shares at 50 commits 25,000 of cash while risking only 1,000.
        assertThat(PositionSizer.positionValue(shares, new BigDecimal("50.00")))
                .isEqualByComparingTo("25000.0000");
    }
}
