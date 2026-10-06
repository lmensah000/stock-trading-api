package com.moneyteam.common.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Token issuing and parsing.
 *
 * The two properties that matter for rotation are that a refresh token carries
 * a jti that survives a round trip, and that the access/refresh distinction
 * holds - an access token must not be redeemable at the refresh endpoint.
 */
class JwtTokenProviderTest {

    private static final String SECRET = "a-test-secret-that-is-at-least-32-bytes-long";

    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET, 900_000L, 604_800_000L);

    @Test
    @DisplayName("a refresh token carries its jti back unchanged")
    void refreshTokenRoundTripsItsJti() {
        String jti = provider.newTokenId();

        Claims claims = provider.parseClaims(
                provider.generateRefreshToken(7L, "ada", "USER", jti));

        assertThat(provider.getTokenId(claims)).isEqualTo(jti);
        assertThat(provider.getUserId(claims)).isEqualTo(7L);
        assertThat(provider.isTokenOfType(claims, JwtTokenProvider.TYPE_REFRESH)).isTrue();
    }

    @Test
    @DisplayName("each generated jti is distinct, so two tokens cannot share a record")
    void tokenIdsAreUnique() {
        assertThat(provider.newTokenId()).isNotEqualTo(provider.newTokenId());
    }

    @Test
    @DisplayName("an access token carries no jti and is not of refresh type")
    void accessTokenIsNotRedeemable() {
        Claims claims = provider.parseClaims(provider.generateAccessToken(7L, "ada", "USER"));

        // Access tokens are not spent against the store, so they carry no id.
        assertThat(provider.getTokenId(claims)).isNull();
        assertThat(provider.isTokenOfType(claims, JwtTokenProvider.TYPE_REFRESH)).isFalse();
        assertThat(provider.isTokenOfType(claims, JwtTokenProvider.TYPE_ACCESS)).isTrue();
    }

    @Test
    @DisplayName("a token signed with a different key is rejected, not parsed")
    void foreignSignatureIsRejected() {
        JwtTokenProvider other =
                new JwtTokenProvider("a-completely-different-secret-over-32-bytes", 900_000L, 1_000L);

        assertThat(provider.parseClaims(
                other.generateRefreshToken(7L, "ada", "USER", "some-jti"))).isNull();
    }

    @Test
    @DisplayName("an expired token parses to null rather than throwing")
    void expiredTokenParsesToNull() throws InterruptedException {
        JwtTokenProvider shortLived = new JwtTokenProvider(SECRET, 1L, 1L);
        String token = shortLived.generateRefreshToken(7L, "ada", "USER", "some-jti");

        Thread.sleep(50);

        assertThat(shortLived.parseClaims(token)).isNull();
        // isTokenOfType must tolerate the null rather than NPE, because that is
        // exactly what the refresh endpoint hands it on an expired token.
        assertThat(shortLived.isTokenOfType(null, JwtTokenProvider.TYPE_REFRESH)).isFalse();
    }

    @Test
    @DisplayName("a garbage string parses to null")
    void malformedTokenParsesToNull() {
        assertThat(provider.parseClaims("not-a-jwt")).isNull();
        assertThat(provider.parseClaims("")).isNull();
    }

    @Test
    @DisplayName("a key under 32 bytes fails at construction rather than issuing weak tokens")
    void shortSecretIsRefused() {
        assertThatThrownBy(() -> new JwtTokenProvider("too-short", 900_000L, 1_000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    @DisplayName("the refresh expiry is exposed so the store can record the same window")
    void refreshTtlIsExposed() {
        // The store duplicates the token's exp into its own row; if these
        // disagreed a token could outlive its record or vice versa.
        assertThat(provider.getRefreshTokenTtlMs()).isEqualTo(604_800_000L);

        Claims claims = provider.parseClaims(
                provider.generateRefreshToken(7L, "ada", "USER", "some-jti"));
        long window = provider.getExpiration(claims).toEpochMilli()
                - provider.getIssuedAt(claims).toEpochMilli();

        // JWT times have second precision, so allow a second of rounding.
        assertThat(window).isCloseTo(604_800_000L, org.assertj.core.data.Offset.offset(1_000L));
    }
}
