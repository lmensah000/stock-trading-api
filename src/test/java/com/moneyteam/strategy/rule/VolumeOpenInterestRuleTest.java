package com.moneyteam.strategy.rule;

import com.moneyteam.marketdata.model.OptionQuote;
import com.moneyteam.strategy.model.Signal;
import com.moneyteam.trading.model.OptionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VolumeOpenInterestRuleTest {

    private static final Instant NOW = Instant.parse("2024-03-04T19:00:00Z");
    private static final Instant OI_YESTERDAY = NOW.minus(Duration.ofHours(18));

    private static OptionQuote quote(long volume, long openInterest, Instant oiAsOf) {
        return new OptionQuote("NVDA", OptionType.CALL, new BigDecimal("900"),
                LocalDate.of(2024, 3, 15),
                new BigDecimal("5.00"), new BigDecimal("5.20"), new BigDecimal("5.10"),
                volume, openInterest, NOW, oiAsOf);
    }

    private static EvaluationContext context(List<OptionQuote> chain) {
        return EvaluationContext.builder("NVDA", NOW).optionChain(chain).build();
    }

    @Test
    @DisplayName("fires when volume is well above open interest on real size")
    void firesOnUnusualVolume() {
        var rule = VolumeOpenInterestRule.standard(); // ratio >= 2, volume >= 500

        Optional<Signal> signal = rule.evaluate(context(List.of(quote(5_000, 1_000, OI_YESTERDAY))));

        assertThat(signal).isPresent();
        assertThat(signal.get().evidence()).containsEntry("volumeToOpenInterestRatio", "5.0000");
    }

    @Test
    @DisplayName("a huge ratio on trivial volume does NOT fire - this is the point of the floor")
    void tinyVolumeIsNotASignal() {
        var rule = VolumeOpenInterestRule.standard();

        // 10 contracts against 2 open interest is a ratio of 5.0 and means nothing.
        assertThat(rule.evaluate(context(List.of(quote(10, 2, OI_YESTERDAY))))).isEmpty();
    }

    @Test
    @DisplayName("heavy volume below the ratio threshold does not fire")
    void heavyVolumeWithLargeOpenInterestDoesNotFire() {
        var rule = VolumeOpenInterestRule.standard();

        // 10k volume but 100k open interest: a ratio of 0.1, entirely ordinary.
        assertThat(rule.evaluate(context(List.of(quote(10_000, 100_000, OI_YESTERDAY))))).isEmpty();
    }

    @Test
    @DisplayName("a ratio exactly at the threshold counts")
    void thresholdIsInclusive() {
        var rule = VolumeOpenInterestRule.standard();

        assertThat(rule.evaluate(context(List.of(quote(2_000, 1_000, OI_YESTERDAY))))).isPresent();
    }

    @Test
    @DisplayName("zero open interest yields no ratio and no signal")
    void zeroOpenInterestIsNotInfiniteConviction() {
        var rule = VolumeOpenInterestRule.standard();

        // Treating 0 as a tiny denominator would rank a brand-new contract
        // above every genuine rotation in the chain.
        assertThat(rule.evaluate(context(List.of(quote(5_000, 0, OI_YESTERDAY))))).isEmpty();
    }

    @Test
    @DisplayName("open interest older than the limit is refused as too stale to divide by")
    void staleOpenInterestIsRefused() {
        var rule = VolumeOpenInterestRule.standard(); // max age 2 days

        assertThat(rule.evaluate(context(List.of(
                quote(5_000, 1_000, NOW.minus(Duration.ofDays(5))))))).isEmpty();
    }

    @Test
    @DisplayName("the signal records how stale its denominator was")
    void evidenceCarriesOpenInterestAge() {
        var rule = VolumeOpenInterestRule.standard();

        Signal signal = rule.evaluate(context(List.of(quote(5_000, 1_000, OI_YESTERDAY)))).orElseThrow();

        assertThat(signal.evidence()).containsEntry("openInterestAgeHours", "18");
        assertThat(signal.evidence()).containsKey("openInterestAsOf");
    }

    @Test
    @DisplayName("the most extreme qualifying contract in the chain is the one reported")
    void picksTheStrongestContract() {
        var rule = VolumeOpenInterestRule.standard();

        Signal signal = rule.evaluate(context(List.of(
                quote(2_000, 1_000, OI_YESTERDAY),   // ratio 2
                quote(9_000, 1_000, OI_YESTERDAY),   // ratio 9  <- strongest
                quote(3_000, 1_000, OI_YESTERDAY)    // ratio 3
        ))).orElseThrow();

        assertThat(signal.evidence()).containsEntry("volumeToOpenInterestRatio", "9.0000");
    }

    @Test
    @DisplayName("an empty chain yields nothing")
    void emptyChain() {
        assertThat(VolumeOpenInterestRule.standard().evaluate(context(List.of()))).isEmpty();
    }
}
