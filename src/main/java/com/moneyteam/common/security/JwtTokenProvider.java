package com.moneyteam.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Issues and validates HS256 JWTs.
 *
 * Access and refresh tokens are distinguished by a "type" claim so that an
 * access token cannot be replayed against the refresh endpoint to mint a fresh
 * pair indefinitely.
 */
@Component
public class JwtTokenProvider {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_USER_ID = "uid";

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey signingKey;
    private final long accessTokenTtlMs;
    private final long refreshTokenTtlMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-ttl-ms:900000}") long accessTokenTtlMs,
            @Value("${app.jwt.refresh-token-ttl-ms:604800000}") long refreshTokenTtlMs) {

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // HS256 requires >= 256 bits. Failing loudly at startup beats silently
            // accepting a weak key that would make every issued token forgeable.
            throw new IllegalStateException(
                    "app.jwt.secret must be at least 32 bytes for HS256; got " + keyBytes.length);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenTtlMs = accessTokenTtlMs;
        this.refreshTokenTtlMs = refreshTokenTtlMs;
    }

    public String generateAccessToken(Long userId, String userName, String role) {
        return buildToken(userId, userName, role, TYPE_ACCESS, accessTokenTtlMs);
    }

    public String generateRefreshToken(Long userId, String userName, String role) {
        return buildToken(userId, userName, role, TYPE_REFRESH, refreshTokenTtlMs);
    }

    private String buildToken(Long userId, String userName, String role, String type, long ttlMs) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(userName)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, type)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ttlMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Returns the token's claims, or null when the token is invalid or expired. */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException ex) {
            // Log the reason, never the token itself.
            log.debug("Rejected JWT: {}", ex.getMessage());
            return null;
        }
    }

    public boolean isTokenOfType(Claims claims, String expectedType) {
        return claims != null && expectedType.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public String getUserName(Claims claims) {
        return claims.getSubject();
    }

    public Long getUserId(Claims claims) {
        Number uid = claims.get(CLAIM_USER_ID, Number.class);
        return uid == null ? null : uid.longValue();
    }

    public String getRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }

    public long getAccessTokenTtlMs() {
        return accessTokenTtlMs;
    }
}
