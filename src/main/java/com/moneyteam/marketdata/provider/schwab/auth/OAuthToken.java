package com.moneyteam.marketdata.provider.schwab.auth;

import javax.persistence.*;
import java.time.Instant;

/**
 * The persisted half of a provider's OAuth state.
 *
 * Only the refresh token is stored, and only in encrypted form. Access tokens
 * live 30 minutes and are held in memory; persisting them would widen the blast
 * radius of a database compromise for no benefit.
 *
 * {@code tokenVersion} backs the compare-and-set that keeps refresh
 * single-flight. See {@link SchwabAuthService} for why that matters.
 */
@Entity
@Table(name = "oauth_tokens")
public class OAuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false, unique = true)
    private String provider;

    @Lob
    @Column(name = "refresh_token_encrypted", nullable = false)
    private byte[] refreshTokenEncrypted;

    @Column(name = "access_token_expires_at")
    private Instant accessTokenExpiresAt;

    @Column(name = "refresh_token_issued_at", nullable = false)
    private Instant refreshTokenIssuedAt;

    @Version
    @Column(name = "token_version", nullable = false)
    private Long tokenVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public byte[] getRefreshTokenEncrypted() {
        return refreshTokenEncrypted;
    }

    public void setRefreshTokenEncrypted(byte[] refreshTokenEncrypted) {
        this.refreshTokenEncrypted = refreshTokenEncrypted;
    }

    public Instant getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    public void setAccessTokenExpiresAt(Instant accessTokenExpiresAt) {
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }

    public Instant getRefreshTokenIssuedAt() {
        return refreshTokenIssuedAt;
    }

    public void setRefreshTokenIssuedAt(Instant refreshTokenIssuedAt) {
        this.refreshTokenIssuedAt = refreshTokenIssuedAt;
    }

    public Long getTokenVersion() {
        return tokenVersion;
    }

    public void setTokenVersion(Long tokenVersion) {
        this.tokenVersion = tokenVersion;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Deliberately omits every token field. This object exists to hold
     * credentials; a toString that printed them would put them in logs.
     */
    @Override
    public String toString() {
        return "OAuthToken{provider=" + provider
                + ", refreshTokenIssuedAt=" + refreshTokenIssuedAt
                + ", tokenVersion=" + tokenVersion + "}";
    }
}
