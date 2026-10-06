package com.moneyteam.common.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenRecord, Long> {

    Optional<RefreshTokenRecord> findByJti(String jti);

    /**
     * Spends a refresh token, returning 1 if this call was the one that spent it
     * and 0 otherwise.
     *
     * This is the single-use guarantee, and it is a single statement on purpose.
     * Reading the row and then updating it would leave a window in which two
     * requests both see {@code usedAt == null} and both proceed. Here InnoDB
     * takes a row lock for the duration, so of two callers presenting the same
     * jti exactly one can observe a row matching the WHERE clause.
     *
     * The caller must treat 0 as a failure and diagnose why. It does not
     * distinguish already-used from revoked from expired - {@link #findByJti}
     * does that afterwards, once the race has already been decided.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshTokenRecord t "
            + "set t.usedAt = :now, t.replacedByJti = :replacementJti "
            + "where t.jti = :jti "
            + "and t.usedAt is null "
            + "and t.revokedAt is null "
            + "and t.expiresAt > :now")
    int spend(@Param("jti") String jti,
              @Param("replacementJti") String replacementJti,
              @Param("now") Instant now);

    /**
     * Revokes every outstanding token for a user, returning the number revoked.
     *
     * Called when a replay is detected. Already-spent rows are stamped too, so
     * the audit trail shows the full extent of what the leak covered rather
     * than only the tokens that happened to still be live.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshTokenRecord t set t.revokedAt = :now "
            + "where t.userId = :userId and t.revokedAt is null")
    int revokeAllForUser(@Param("userId") Long userId, @Param("now") Instant now);

    /**
     * Drops rows whose tokens have expired. Once past expiry the signature no
     * longer verifies, so the row can no longer affect a decision and only
     * costs space.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from RefreshTokenRecord t where t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);

    long countByUserIdAndUsedAtIsNullAndRevokedAtIsNull(Long userId);
}
