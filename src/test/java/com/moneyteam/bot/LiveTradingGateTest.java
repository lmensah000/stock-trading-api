package com.moneyteam.bot;

import com.moneyteam.trading.model.Account;
import com.moneyteam.trading.model.Position;
import com.moneyteam.trading.model.enums.OrderSide;
import com.moneyteam.trading.repository.AccountRepository;
import com.moneyteam.trading.repository.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * The gate between the strategy and real money.
 *
 * Two behaviours matter more than the rest, and both are the kind that only
 * fail in the situation you least want them to. A SELL must never be blocked,
 * because blocking a close traps an open position in a falling market. And
 * reaching the equity target must *not* by itself unlock anything, because that
 * would make any arithmetic error in the equity calculation an error that
 * trades real money.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LiveTradingGateTest {

    private static final Instant NOW = Instant.parse("2026-03-04T19:00:00Z");
    private static final long OWNER = 9L;
    private static final BigDecimal START = new BigDecimal("100000.0000");
    private static final BigDecimal TARGET = new BigDecimal("130000.0000");

    @Mock private BotConfigRepository configRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private PositionRepository positionRepository;

    private LiveTradingGate gate;

    @BeforeEach
    void setUp() {
        gate = new LiveTradingGate(configRepository, accountRepository, positionRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(configRepository.save(any(BotConfig.class))).thenAnswer(i -> i.getArgument(0));
        when(positionRepository.findByUserRefId(OWNER)).thenReturn(List.of());
    }

    private BotConfig config() {
        return new BotConfig(OWNER, START, TARGET, NOW);
    }

    private void configExists(BotConfig config) {
        when(configRepository.findSingleton()).thenReturn(Optional.of(config));
    }

    private void cashIs(String amount) {
        Account account = new Account();
        account.setUserId(OWNER);
        account.setCashBalance(new BigDecimal(amount));
        when(accountRepository.findByUserId(OWNER)).thenReturn(Optional.of(account));
    }

    // ---------------------------------------------------------------
    //  Closing is never blocked
    // ---------------------------------------------------------------

    @Test
    @DisplayName("a SELL is allowed while locked, so a position can never be trapped")
    void sellIsNeverBlocked() {
        configExists(config());

        assertThatCode(() -> gate.assertCanPlace(OrderSide.SELL)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a SELL is allowed even with no configuration at all")
    void sellIsAllowedWithNoConfig() {
        when(configRepository.findSingleton()).thenReturn(Optional.empty());

        // An uninitialised bot must still be able to reduce exposure. This is
        // the case where a strict gate would do the most damage.
        assertThatCode(() -> gate.assertCanPlace(OrderSide.SELL)).doesNotThrowAnyException();
    }

    // ---------------------------------------------------------------
    //  Opening risk
    // ---------------------------------------------------------------

    @Test
    @DisplayName("a BUY is allowed in paper mode: the paper broker is the normal path")
    void buyIsAllowedInPaperMode() {
        configExists(config());

        assertThatCode(() -> gate.assertCanPlace(OrderSide.BUY)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a BUY is refused when the bot has no configuration")
    void buyIsRefusedWithoutConfig() {
        when(configRepository.findSingleton()).thenReturn(Optional.empty());

        // Not knowing the rules is a reason to open no new risk.
        assertThatThrownBy(() -> gate.assertCanPlace(OrderSide.BUY))
                .isInstanceOf(LiveTradingLockedException.class)
                .hasMessageContaining("no configuration");
    }

    @Test
    @DisplayName("mode LIVE without an unlock is treated as locked, not as live")
    void liveModeWithoutUnlockIsRefused() {
        BotConfig config = config();
        config.arm(NOW);
        config.unlock(NOW);
        config.relock(NOW);
        // relock clears unlockedAt; force the inconsistent state directly.
        setField(config, "mode", TradingMode.LIVE);
        configExists(config);

        assertThatThrownBy(() -> gate.assertCanPlace(OrderSide.BUY))
                .isInstanceOf(LiveTradingLockedException.class)
                .hasMessageContaining("never unlocked");
    }

    // ---------------------------------------------------------------
    //  Arming is not unlocking
    // ---------------------------------------------------------------

    @Test
    @DisplayName("equity below the target does not arm the gate")
    void belowTargetDoesNotArm() {
        BotConfig config = config();
        configExists(config);
        cashIs("129999.9999");

        assertThat(gate.evaluate()).isFalse();
        assertThat(config.isArmed()).isFalse();
    }

    @Test
    @DisplayName("equity at the target arms the gate but does NOT unlock it")
    void atTargetArmsButDoesNotUnlock() {
        BotConfig config = config();
        configExists(config);
        cashIs("130000.0000");

        assertThat(gate.evaluate()).isTrue();
        assertThat(config.isArmed()).isTrue();

        // The whole safety argument lives in this assertion.
        assertThat(config.isLive()).isFalse();
        assertThat(config.getUnlockedAt()).isNull();
        assertThat(config.getMode()).isEqualTo(TradingMode.PAPER);
    }

    @Test
    @DisplayName("arming is idempotent and keeps the original timestamp")
    void armingIsIdempotent() {
        BotConfig config = config();
        configExists(config);
        cashIs("200000.0000");

        assertThat(gate.evaluate()).isTrue();
        Instant firstArmedAt = config.getArmedAt();

        assertThat(gate.evaluate()).isFalse();
        assertThat(config.getArmedAt()).isEqualTo(firstArmedAt);
    }

    @Test
    @DisplayName("unlocking is refused while the gate is unarmed")
    void unlockRefusedWhileUnarmed() {
        configExists(config());
        cashIs("100000.0000");

        assertThatThrownBy(() -> gate.unlock())
                .isInstanceOf(LiveTradingLockedException.class)
                .hasMessageContaining("not armed");
    }

    @Test
    @DisplayName("unlocking after arming flips to live, and only then")
    void unlockAfterArming() {
        BotConfig config = config();
        configExists(config);
        cashIs("130000.0000");

        gate.evaluate();
        gate.unlock();

        assertThat(config.getMode()).isEqualTo(TradingMode.LIVE);
        assertThat(config.isLive()).isTrue();
        assertThatCode(() -> gate.assertCanPlace(OrderSide.BUY)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("relocking needs no condition, because reducing risk is never gated")
    void relockIsUnconditional() {
        BotConfig config = config();
        configExists(config);
        cashIs("130000.0000");
        gate.evaluate();
        gate.unlock();

        gate.relock();

        assertThat(config.getMode()).isEqualTo(TradingMode.PAPER);
        assertThat(config.isLive()).isFalse();
    }

    // ---------------------------------------------------------------
    //  Equity
    // ---------------------------------------------------------------

    @Test
    @DisplayName("equity counts open positions at cost, so an invested account can still arm")
    void equityIncludesOpenPositionsAtCost() {
        BotConfig config = config();
        configExists(config);
        // Fully invested: cash is almost gone, but the money is in a position.
        cashIs("5000.0000");
        when(positionRepository.findByUserRefId(OWNER)).thenReturn(List.of(
                position("AAPL", 1000.0, "125.00")));

        // 5,000 cash + 125,000 at cost = 130,000.
        assertThat(gate.paperEquity(OWNER)).isEqualByComparingTo("130000.0000");
        assertThat(gate.evaluate()).isTrue();
    }

    @Test
    @DisplayName("an unrealised gain cannot arm the gate")
    void unrealisedGainDoesNotArm() {
        BotConfig config = config();
        configExists(config);
        cashIs("5000.0000");

        Position position = position("AAPL", 1000.0, "100.00");
        // A large paper profit that has not been booked.
        position.setUnrealizedPnL(new BigDecimal("40000.00"));
        when(positionRepository.findByUserRefId(OWNER)).thenReturn(List.of(position));

        // Cost basis is 105,000, well short of the target, even though marking
        // the position to market would clear it. Only booked money counts.
        assertThat(gate.paperEquity(OWNER)).isEqualByComparingTo("105000.0000");
        assertThat(gate.evaluate()).isFalse();
    }

    @Test
    @DisplayName("a position with no quantity or price contributes nothing rather than failing")
    void incompletePositionIsIgnored() {
        configExists(config());
        cashIs("1000.0000");
        when(positionRepository.findByUserRefId(OWNER))
                .thenReturn(List.of(position("AAPL", null, null)));

        assertThat(gate.paperEquity(OWNER)).isEqualByComparingTo("1000.0000");
    }

    @Test
    @DisplayName("equity is zero when there is no account yet")
    void noAccountMeansZeroEquity() {
        when(accountRepository.findByUserId(OWNER)).thenReturn(Optional.empty());

        assertThat(gate.paperEquity(OWNER)).isEqualByComparingTo("0");
    }

    private static Position position(String ticker, Double quantity, String averagePrice) {
        Position position = new Position();
        position.setStockTicker(ticker);
        position.setTotalQuantity(quantity);
        position.setAveragePrice(averagePrice == null ? null : new BigDecimal(averagePrice));
        return position;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
