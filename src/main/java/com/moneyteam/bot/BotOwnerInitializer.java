package com.moneyteam.bot;

import com.moneyteam.trading.model.Account;
import com.moneyteam.trading.service.AccountService;
import com.moneyteam.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Prepares the bot's account and gate on startup, once.
 *
 * Runs only when {@code app.bot.enabled=true}, so a default checkout starts
 * with no bot state at all. Everything it does is idempotent: a second boot
 * finds the account and the configuration already present and changes nothing,
 * which matters because a container restart is a normal event and must not
 * re-fund the account or reset the gate.
 *
 * <h2>It will not create the user</h2>
 *
 * Creating the owner would mean inventing a password, and a password this class
 * chose would be a default credential - the exact category of mistake that put
 * compromised Schwab keys in this repository's history. So the owner must
 * already be registered, and startup fails with instructions if they are not.
 * Failing loudly here follows the same reasoning as the JWT secret length
 * check: a misconfigured security-relevant value should stop the application
 * rather than be quietly worked around.
 */
@Component
@ConditionalOnProperty(name = "app.bot.enabled", havingValue = "true")
public class BotOwnerInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BotOwnerInitializer.class);

    private final BotIdentity botIdentity;
    private final BotConfigRepository configRepository;
    private final AccountService accountService;
    private final Clock clock;
    private final BigDecimal startingEquity;
    private final BigDecimal unlockTargetEquity;

    public BotOwnerInitializer(BotIdentity botIdentity,
                               BotConfigRepository configRepository,
                               AccountService accountService,
                               Clock clock,
                               @Value("${app.bot.starting-equity:100000}") BigDecimal startingEquity,
                               @Value("${app.bot.unlock-target-equity:}") String unlockTargetEquity) {
        this.botIdentity = botIdentity;
        this.configRepository = configRepository;
        this.accountService = accountService;
        this.clock = clock;
        this.startingEquity = startingEquity;
        this.unlockTargetEquity = parseTarget(unlockTargetEquity);
    }

    private static BigDecimal parseTarget(String raw) {
        if (raw == null || raw.isBlank()) {
            // Null here is not an error yet - it only matters if no
            // configuration row already exists. run() decides.
            return null;
        }
        return new BigDecimal(raw.trim());
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User owner = resolveOwner();

        Account account = ensureFundedAccount(owner.getId());
        ensureConfig(owner.getId());

        BotConfig config = configRepository.findSingleton().orElseThrow();
        log.info("Bot owner is '{}' (userId={}). Mode={}, equity={}, unlock target={}, armed={}.",
                owner.getUserName(), owner.getId(), config.getMode(),
                account.getCashBalance(), config.getUnlockTargetEquity(), config.isArmed());

        if (!config.isLive()) {
            log.info("Live trading is LOCKED. No order can reach a real brokerage account.");
        }
    }

    private User resolveOwner() {
        if (!botIdentity.isConfigured()) {
            throw new IllegalStateException(
                    "app.bot.enabled is true but no owner is set. "
                            + "Set app.bot.owner-username (BOT_OWNER_USERNAME).");
        }
        try {
            return botIdentity.owner();
        } catch (NoSuchElementException e) {
            // Deliberately not created here: that would require inventing a
            // password, and a password chosen by this class is a default
            // credential.
            throw new IllegalStateException(
                    "The bot owner '" + botIdentity.ownerUserName() + "' is not registered. "
                            + "Register them first, then restart:\n"
                            + "  curl -X POST http://localhost:8080/api/users/register \\\n"
                            + "       -H 'Content-Type: application/json' \\\n"
                            + "       -d '{\"userName\":\"" + botIdentity.ownerUserName()
                            + "\",\"passWord\":\"<a password you choose>\"}'", e);
        }
    }

    /**
     * Creates the paper account and funds it once.
     *
     * The deposit is guarded by whether the account already existed, not by
     * whether its balance looks low. Topping up to a target balance would
     * silently refill the account after a losing run, which would make both the
     * equity figure and the gate meaningless.
     */
    private Account ensureFundedAccount(Long ownerUserId) {
        Optional<Account> existing = accountService.getAccount(ownerUserId);
        if (existing.isPresent()) {
            return existing.get();
        }

        accountService.createAccountForUser(ownerUserId);
        Account funded = accountService.deposit(ownerUserId, startingEquity);
        log.info("Created a paper account for userId={} funded with {}.", ownerUserId, startingEquity);
        return funded;
    }

    /**
     * Creates the single configuration row on first boot and never touches it
     * again. The unlock target is captured here and is immutable thereafter -
     * that is what stops the bar being lowered later.
     */
    private void ensureConfig(Long ownerUserId) {
        if (configRepository.findSingleton().isPresent()) {
            return;
        }

        if (unlockTargetEquity == null) {
            throw new IllegalStateException(
                    "app.bot.unlock-target-equity must be set on first boot. It is the paper "
                            + "equity that has to be reached before live trading can be unlocked, "
                            + "and it is recorded once and then immutable - so it has to be chosen "
                            + "deliberately now rather than defaulted to something convenient.");
        }
        if (unlockTargetEquity.compareTo(startingEquity) <= 0) {
            throw new IllegalStateException(
                    "app.bot.unlock-target-equity (" + unlockTargetEquity + ") must be above the "
                            + "starting equity (" + startingEquity + "), or the gate would be "
                            + "armed the moment it was created.");
        }

        configRepository.save(new BotConfig(
                ownerUserId, startingEquity, unlockTargetEquity, clock.instant()));

        log.warn("Live-trading gate created. Paper equity must reach {} before it can be armed, "
                        + "and unlocking then still requires an explicit action.",
                unlockTargetEquity);
    }
}
