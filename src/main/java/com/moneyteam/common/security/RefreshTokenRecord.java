package com.moneyteam.common.security;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

/**
 * The record of one issued refresh token.
 *
 * The token itself is deliberately absent. Only its {@code jti} is stored - a
 * random UUID from the JWT's id claim, which carries no authority on its own.
 * The signature proves the token is genuine; this row answers the separate
 * question of whether it has already been spent.
 *
 * A row is immutable except for {@code usedAt}, {@code revokedAt} and
 * {@code replacedByJti}, and all three are written through the conditional
 * updates on {@link RefreshTokenRepository} rather than by setter, so that the
 * single-use guarantee is enforced by the database rather than by sequencing in
 * application code.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jti", nullable = false, unique = true, length = 36)
    private String jti;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_jti", length = 36)
    private String replacedByJti;

    protected RefreshTokenRecord() {
        // for JPA
    }

    public RefreshTokenRecord(String jti, Long userId, Instant issuedAt, Instant expiresAt) {
        this.jti = jti;
        this.userId = userId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public String getJti() {
        return jti;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getReplacedByJti() {
        return replacedByJti;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    @Override
    public String toString() {
        // The jti is not a credential, so it is safe to log and is the only
        // handle that makes a replay traceable.
        return "RefreshTokenRecord{jti=" + jti
                + ", userId=" + userId
                + ", expiresAt=" + expiresAt
                + ", usedAt=" + usedAt
                + ", revokedAt=" + revokedAt + "}";
    }
}
