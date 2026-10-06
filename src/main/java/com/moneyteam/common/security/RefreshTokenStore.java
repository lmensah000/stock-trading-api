package com.moneyteam.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Makes refresh tokens single-use.
 *
 * A refresh token that can be redeemed more than once is a credential with a
 * seven-day life and no way to retire it early: a copy taken from a log, a
 * proxy or a backup keeps working until it expires on its own, and the
 * legitimate holder sees nothing unusual because their own refreshes keep
 * succeeding too. Recording each issued token and spending it on redemption is
 * what turns that into a credential that stops working the moment it is used.
 *
 * It also makes theft detectable. Once each token may be spent once, a second
 * redemption of the same token means two parties hold it. The server cannot
 * tell which one is the owner, so {@link Outcome#REPLAYED} revokes the user's
 * whole set and forces a fresh login. That follows the OAuth 2.0 security best
 * current practice (RFC 9700) on refresh token replay, and it is the reason
 * this class revokes a family rather than just refusing one call.
 *
 * Every method returns an {@link Outcome} rather than throwing. Revocation has
 * to be committed, and a runtime exception thrown from inside the transaction
 * that performed it would roll it back - leaving the thief's token live, which
 * is precisely the case this class exists to handle. Translating an outcome
 * into an HTTP response is the caller's job.
 */
@Service
public class RefreshTokenStore {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenStore.class);

    /** What happened when a token was presented for redemption. */
    public enum Outcome {
        /** Spent by this call. The caller may issue a replacement pair. */
        SPENT,
        /**
         * Already spent, or spent concurrently by another request. Treated as
         * a leak: the user's outstanding tokens have been revoked.
         */
        REPLAYED,
        /** Part of a family already revoked by an earlier replay or a logout. */
        REVOKED,
        /** Past its expiry. Ordinary, and not a sign of anything wrong. */
        EXPIRED,
        /**
         * Correctly signed but with no record. Either it was issued before
         * rotation existed, or its row was purged after expiry.
         */
        UNKNOWN
    }

    private final RefreshTokenRepository repository;
    private final Clock clock;

    public RefreshTokenStore(RefreshTokenRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Records a newly issued refresh token so that it can be spent later.
     *
     * Must be called before the token reaches the client. A token handed out
     * without a row would be rejected as {@link Outcome#UNKNOWN} on first use.
     */
    @Transactional
    public void register(String jti, Long userId, Instant issuedAt, Instant expiresAt) {
        repository.save(new RefreshTokenRecord(jti, userId, issuedAt, expiresAt));
    }

    /**
     * Spends the token identified by {@code jti}, and on a replay revokes
     * everything else the user holds.
     *
     * @param replacementJti the jti of the token being issued in exchange,
     *                       recorded for audit so a replay can be traced back
     *                       along the chain to the login that began it
     */
    @Transactional
    public Outcome consume(String jti, String replacementJti) {
        Instant now = clock.instant();

        Optional<RefreshTokenRecord> found = repository.findByJti(jti);
        if (found.isEmpty()) {
            log.warn("Refresh token with no stored record was presented; jti={}", jti);
            return Outcome.UNKNOWN;
        }

        RefreshTokenRecord record = found.get();
        if (record.isRevoked()) {
            return Outcome.REVOKED;
        }
        if (!record.getExpiresAt().isAfter(now)) {
            return Outcome.EXPIRED;
        }
        if (record.isUsed()) {
            return replay(record, now);
        }

        // The conditional update is the gate, not the checks above. Two requests
        // can both reach this line having seen an unspent row; only the one
        // whose statement matches gets a row back.
        if (repository.spend(jti, replacementJti, now) == 1) {
            return Outcome.SPENT;
        }

        // Lost the race, so this token was spent between the read and here.
        // Indistinguishable from a replay, and handled as one.
        return replay(record, now);
    }

    private Outcome replay(RefreshTokenRecord record, Instant now) {
        int revoked = repository.revokeAllForUser(record.getUserId(), now);
        log.warn("Refresh token replay detected for userId={}; jti={} was already spent. "
                        + "Revoked {} token(s) for this user; a fresh login is now required.",
                record.getUserId(), record.getJti(), revoked);
        return Outcome.REPLAYED;
    }

    /** Revokes everything a user holds. Used on logout and on replay. */
    @Transactional
    public int revokeAllForUser(Long userId) {
        return repository.revokeAllForUser(userId, clock.instant());
    }

    /**
     * Drops rows for tokens that have expired.
     *
     * An expired token fails signature validation before this table is ever
     * consulted, so its row cannot change a decision and only accumulates.
     * Hourly is far more often than needed for a seven-day TTL and keeps each
     * delete small.
     */
    @Scheduled(fixedDelayString = "${app.jwt.refresh-token-purge-interval-ms:3600000}")
    @Transactional
    public int purgeExpired() {
        int deleted = repository.deleteExpiredBefore(clock.instant());
        if (deleted > 0) {
            log.info("Purged {} expired refresh token record(s)", deleted);
        }
        return deleted;
    }
}
