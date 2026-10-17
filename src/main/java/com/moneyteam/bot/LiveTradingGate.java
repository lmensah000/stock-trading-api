package com.moneyteam.bot;

import com.moneyteam.trading.model.Account;
import com.moneyteam.trading.model.Position;
import com.moneyteam.trading.model.enums.OrderSide;
import com.moneyteam.trading.repository.AccountRepository;
import com.moneyteam.trading.repository.PositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Stands between the strategy and real money.
 *
 * Live trading is unreachable until two separate things have happened: paper
 * equity has reached a target fixed at the very first boot, and the owner has
 * explicitly unlocked it. The first is observed, the second is decided. Keeping
 * them apart is the point - if reaching the target opened the gate by itself,
 * then any arithmetic error in the equity calculation would be an error that
 * trades real money.
 *
 * <h2>What this does not do</h2>
 *
 * This gate asks one question: may new risk be opened with real money. It is
 * not the market-stage gate. {@code CLAUDE.md} specifies an exposure ceiling by
 * market stage as a hard cap in the order path, and that does not exist yet.
 * Nothing here should be read as evidence that the strategy's own risk rules
 * are enforced, and the market-stage cap must be built before live trading is a
 * sensible thing to unlock at all.
 *
 * <h2>Why SELL is never blocked</h2>
 *
 * Blocking a close would trap an open position. That mirrors the rule the
 * exposure ceiling has to follow: a cap of zero means open no new risk, never
 * that existing risk cannot be reduced. A gate that can strand a position in a
 * falling market is more dangerous than no gate at all, so {@link #assertCanPlace}
 * gates BUY only and lets SELL through unconditionally.
 */
@Service
public class LiveTradingGate {

    private static final Logger log = LoggerFactory.getLogger(LiveTradingGate.class);

    private final BotConfigRepository configRepository;
    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final Clock clock;

    public LiveTradingGate(BotConfigRepository configRepository,
                           AccountRepository accountRepository,
                           PositionRepository positionRepository,
                           Clock clock) {
        this.configRepository = configRepository;
        this.accountRepository = accountRepository;
        this.positionRepository = positionRepository;
        this.clock = clock;
    }

    /**
     * Throws when the given side may not be placed in the current mode.
     *
     * SELL always passes. BUY passes in {@link TradingMode#PAPER} - the paper
     * broker is the normal path and is not restricted - and in LIVE only once
     * the gate has actually been unlocked.
     *
     * With no configuration row at all, BUY is refused. An absent row means the
     * bot has never been initialised, and the safe reading of "I do not know
     * what the rules are" is to open no new risk.
     */
    @Transactional(readOnly = true)
    public void assertCanPlace(OrderSide side) {
        if (side == OrderSide.SELL) {
            // Never gated. Reducing exposure must always be possible.
            return;
        }

        Optional<BotConfig> config = configRepository.findSingleton();
        if (config.isEmpty()) {
            throw new LiveTradingLockedException(
                    "The bot has no configuration, so no position may be opened. "
                            + "Set app.bot.unlock-target-equity and start with app.bot.enabled=true.");
        }

        BotConfig botConfig = config.get();
        if (botConfig.getMode() == TradingMode.LIVE && botConfig.getUnlockedAt() == null) {
            // Mode says LIVE but nothing ever unlocked it. Treat the stricter
            // of the two signals as the truth rather than trusting the mode
            // column alone.
            throw new LiveTradingLockedException(
                    "Live mode is set but was never unlocked. Refusing to open a position.");
        }
    }

    /**
     * Records progress toward the target, arming the gate when it is reached.
     *
     * Arming is not unlocking. This only flips a flag saying the condition has
     * been met; {@link #unlock()} is still required and only the owner can call
     * it. Safe to call after every equity change - it is idempotent once armed.
     *
     * @return true if this call was the one that armed the gate
     */
    @Transactional
    public boolean evaluate() {
        Optional<BotConfig> found = configRepository.findSingleton();
        if (found.isEmpty()) {
            return false;
        }

        BotConfig config = found.get();
        if (config.isArmed()) {
            return false;
        }

        BigDecimal equity = paperEquity(config.getOwnerUserId());
        if (equity.compareTo(config.getUnlockTargetEquity()) < 0) {
            return false;
        }

        Instant now = clock.instant();
        config.arm(now);
        configRepository.save(config);

        log.warn("Paper equity {} has reached the target {}. The live-trading gate is now ARMED. "
                        + "It is still LOCKED: live trading requires an explicit unlock.",
                equity, config.getUnlockTargetEquity());
        return true;
    }

    /**
     * Opens the gate. Refuses unless it has been armed.
     *
     * Deliberately has no automatic caller anywhere in the codebase. The only
     * way to reach real money is for the owner to invoke this on purpose.
     */
    @Transactional
    public BotConfig unlock() {
        BotConfig config = configRepository.findSingleton()
                .orElseThrow(() -> new LiveTradingLockedException("The bot has no configuration to unlock."));

        if (!config.isArmed()) {
            throw new LiveTradingLockedException(
                    "The live-trading gate is not armed. Paper equity of "
                            + paperEquity(config.getOwnerUserId())
                            + " has not reached the target of " + config.getUnlockTargetEquity() + ".");
        }

        if (config.isLive()) {
            return config;
        }

        config.unlock(clock.instant());
        BotConfig saved = configRepository.save(config);

        log.warn("LIVE TRADING UNLOCKED. Orders may now reach a real brokerage account. "
                + "Note that no live broker adapter is implemented, so this grants no "
                + "execution path today, and the market-stage exposure ceiling is still "
                + "not enforced in the order path.");
        return saved;
    }

    /**
     * Returns to paper. Always permitted and never requires a condition -
     * reducing the blast radius must not itself be gated.
     */
    @Transactional
    public BotConfig relock() {
        BotConfig config = configRepository.findSingleton()
                .orElseThrow(() -> new LiveTradingLockedException("The bot has no configuration to relock."));

        config.relock(clock.instant());
        log.warn("Live trading relocked. Mode is back to PAPER.");
        return configRepository.save(config);
    }

    /**
     * Equity of the owner's paper account: cash plus the cost basis of open
     * positions.
     *
     * Open positions have to be counted. Cash alone would mean a fully invested
     * account showed almost nothing and could never arm, however well it was
     * doing - the gate would be unreachable by exactly the accounts that had
     * earned their way through it.
     *
     * Cost basis rather than market value is deliberate, for two reasons. It
     * needs no market data, so arming cannot depend on whether a quote happened
     * to be available. And it counts only money actually booked, so an
     * unrealised spike on an open position cannot arm the gate - the equity
     * figure can then only be reached by closing trades at a profit, which is
     * the thing being tested. It understates a winning open position, which
     * delays arming and never hastens it; for a gate guarding real money, that
     * is the correct direction to be wrong in.
     */
    @Transactional(readOnly = true)
    public BigDecimal paperEquity(Long ownerUserId) {
        BigDecimal cash = accountRepository.findByUserId(ownerUserId)
                .map(Account::getCashBalance)
                .orElse(BigDecimal.ZERO);

        BigDecimal positionsAtCost = positionRepository.findByUserRefId(ownerUserId).stream()
                .map(LiveTradingGate::costBasis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return cash.add(positionsAtCost);
    }

    private static BigDecimal costBasis(Position position) {
        Double quantity = position.getTotalQuantity();
        BigDecimal averagePrice = position.getAveragePrice();
        if (quantity == null || averagePrice == null) {
            return BigDecimal.ZERO;
        }
        // Quantity becomes money the moment it is multiplied by a price, so the
        // result is BigDecimal with an explicit scale. See the money boundary
        // rule in CLAUDE.md.
        return averagePrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(4, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public Optional<BotConfig> currentConfig() {
        return configRepository.findSingleton();
    }
}
