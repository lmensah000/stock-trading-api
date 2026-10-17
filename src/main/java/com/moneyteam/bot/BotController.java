package com.moneyteam.bot;

import com.moneyteam.common.security.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Status of the bot, and the one place live trading can be unlocked.
 *
 * Unlocking is a deliberate act with no automatic caller anywhere, which is why
 * it is an endpoint rather than something the gate does for itself when the
 * target is reached. Reaching the target only arms it.
 *
 * The request must echo the exact unlock target as confirmation. That is not
 * authentication - the caller is already authenticated - it is there so an
 * unlock cannot be a mistake. Typing the number back is the difference between
 * deciding to trade real money and clicking the wrong thing.
 */
@RestController
@RequestMapping("/api/bot")
public class BotController {

    private static final Logger log = LoggerFactory.getLogger(BotController.class);

    private final LiveTradingGate gate;
    private final CurrentUserService currentUserService;

    public BotController(LiveTradingGate gate, CurrentUserService currentUserService) {
        this.gate = gate;
        this.currentUserService = currentUserService;
    }

    /** Confirmation payload for {@link #unlock}. */
    public static class UnlockRequest {

        @NotNull
        private BigDecimal confirmTargetEquity;

        public BigDecimal getConfirmTargetEquity() {
            return confirmTargetEquity;
        }

        public void setConfirmTargetEquity(BigDecimal confirmTargetEquity) {
            this.confirmTargetEquity = confirmTargetEquity;
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return gate.currentConfig()
                .map(config -> ResponseEntity.ok(Map.<String, Object>of(
                        "mode", config.getMode(),
                        "armed", config.isArmed(),
                        "live", config.isLive(),
                        "startingEquity", config.getStartingEquity(),
                        "currentEquity", gate.paperEquity(config.getOwnerUserId()),
                        "unlockTargetEquity", config.getUnlockTargetEquity())))
                .orElseGet(() -> ResponseEntity.ok(Map.of(
                        "mode", "UNCONFIGURED",
                        "armed", false,
                        "live", false)));
    }

    /**
     * Opens the gate. Refused unless it has been armed by reaching the target,
     * and unless the request echoes that target exactly.
     */
    @PostMapping("/unlock")
    public ResponseEntity<Map<String, Object>> unlock(@Valid @RequestBody UnlockRequest request) {
        BotConfig config = gate.currentConfig()
                .orElseThrow(() -> new LiveTradingLockedException("The bot has no configuration to unlock."));

        if (config.getUnlockTargetEquity().compareTo(request.getConfirmTargetEquity()) != 0) {
            throw new LiveTradingLockedException(
                    "Confirmation does not match the unlock target. Send the exact target equity "
                            + "to confirm that unlocking real-money trading is intended.");
        }

        BotConfig unlocked = gate.unlock();
        log.warn("Live trading unlocked by user '{}'.", currentUserService.getCurrentUser().getUserName());

        return ResponseEntity.ok(Map.of(
                "mode", unlocked.getMode(),
                "live", unlocked.isLive(),
                "unlockedAt", String.valueOf(unlocked.getUnlockedAt()),
                "warning", "No live broker adapter is implemented, so no order can reach a "
                        + "brokerage account yet. The market-stage exposure ceiling is also not "
                        + "enforced in the order path."));
    }

    /** Returns to paper. Needs no confirmation: reducing risk is never gated. */
    @PostMapping("/relock")
    public ResponseEntity<Map<String, Object>> relock() {
        BotConfig config = gate.relock();
        log.warn("Live trading relocked by user '{}'.", currentUserService.getCurrentUser().getUserName());
        return ResponseEntity.ok(Map.of("mode", config.getMode(), "live", config.isLive()));
    }
}
