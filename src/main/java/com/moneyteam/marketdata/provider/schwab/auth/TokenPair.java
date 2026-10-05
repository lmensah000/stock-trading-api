package com.moneyteam.marketdata.provider.schwab.auth;

import java.time.Duration;
import java.time.Instant;

/**
 * A freshly issued access/refresh pair.
 *
 * Not persisted as-is: only the refresh token is stored, encrypted. The access
 * token stays in memory for its 30-minute life.
 */
public record TokenPair(
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        Instant refreshTokenIssuedAt) {

    /** Schwab refresh tokens last seven days from issue. */
    public static final Duration REFRESH_TOKEN_LIFETIME = Duration.ofDays(7);

    public Instant refreshTokenExpiresAt() {
        return refreshTokenIssuedAt.plus(REFRESH_TOKEN_LIFETIME);
    }

    public boolean accessTokenExpired(Instant now, Duration skew) {
        return !now.plus(skew).isBefore(accessTokenExpiresAt);
    }
}
