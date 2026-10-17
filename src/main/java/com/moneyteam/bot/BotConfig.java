package com.moneyteam.bot;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * The bot's operating mode and the condition under which real money becomes
 * reachable. Exactly one row exists; see {@code V5__bot_config.sql} for how the
 * schema enforces that.
 *
 * {@code unlockTargetEquity} has no setter. That is not an oversight: the
 * threshold is written once at construction and the absence of a setter is what
 * makes it immutable. A gate whose bar can be lowered on the evening it would
 * have stopped a trade is decorative, so lowering it has to mean editing the
 * database by hand rather than calling a method that happens to be there.
 *
 * {@code armed} and {@code unlockedAt} are deliberately distinct. Reaching the
 * target arms the gate; only the owner opens it. If the two were one field, an
 * arithmetic error in the equity calculation would be a live-money error.
 */
@Entity
@Table(name = "bot_config")
public class BotConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Constant, with a unique constraint, so only one row can ever exist. */
    @Column(name = "singleton", nullable = false)
    private Integer singleton = 1;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    /**
     * Stored as a string so the value is readable in the database and so
     * reordering the enum cannot silently change what an existing row means.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 16)
    private TradingMode mode = TradingMode.PAPER;

    @Column(name = "starting_equity", nullable = false)
    private BigDecimal startingEquity;

    @Column(name = "unlock_target_equity", nullable = false, updatable = false)
    private BigDecimal unlockTargetEquity;

    @Column(name = "armed", nullable = false)
    private boolean armed;

    @Column(name = "armed_at")
    private Instant armedAt;

    @Column(name = "unlocked_at")
    private Instant unlockedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BotConfig() {
        // for JPA
    }

    public BotConfig(Long ownerUserId, BigDecimal startingEquity,
                     BigDecimal unlockTargetEquity, Instant now) {
        this.ownerUserId = ownerUserId;
        this.startingEquity = startingEquity;
        this.unlockTargetEquity = unlockTargetEquity;
        this.mode = TradingMode.PAPER;
        this.armed = false;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** True once paper equity has reached the target. Does not mean unlocked. */
    public boolean isArmed() {
        return armed;
    }

    /**
     * True only when live trading has been explicitly unlocked. This is the
     * single question the order path asks.
     */
    public boolean isLive() {
        return mode == TradingMode.LIVE && unlockedAt != null;
    }

    void arm(Instant now) {
        if (!armed) {
            this.armed = true;
            this.armedAt = now;
            this.updatedAt = now;
        }
    }

    void unlock(Instant now) {
        this.mode = TradingMode.LIVE;
        this.unlockedAt = now;
        this.updatedAt = now;
    }

    void relock(Instant now) {
        this.mode = TradingMode.PAPER;
        this.unlockedAt = null;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public TradingMode getMode() {
        return mode;
    }

    public BigDecimal getStartingEquity() {
        return startingEquity;
    }

    public BigDecimal getUnlockTargetEquity() {
        return unlockTargetEquity;
    }

    public Instant getArmedAt() {
        return armedAt;
    }

    public Instant getUnlockedAt() {
        return unlockedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "BotConfig{ownerUserId=" + ownerUserId
                + ", mode=" + mode
                + ", startingEquity=" + startingEquity
                + ", unlockTargetEquity=" + unlockTargetEquity
                + ", armed=" + armed
                + ", unlockedAt=" + unlockedAt + "}";
    }
}
