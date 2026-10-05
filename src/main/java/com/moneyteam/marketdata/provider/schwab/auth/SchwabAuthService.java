package com.moneyteam.marketdata.provider.schwab.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Owns the Schwab OAuth token lifecycle.
 *
 * <h2>The two failure modes this is built around</h2>
 *
 * Schwab access tokens last 30 minutes. Refresh tokens last seven days and are
 * <strong>consumed on use</strong>: every refresh spends the stored token and
 * returns a replacement carrying a fresh seven days. That single property
 * creates both ways this integration normally breaks.
 *
 * <p><strong>1. Concurrent refresh is destructive, not just wasteful.</strong>
 * A scan runs one task per ticker, so when the access token expires every
 * in-flight task sees 401 at the same moment. If each refreshes, they race to
 * spend the same one-time token: one wins and the rest receive an error for a
 * token that has already been consumed. Worse, a loser may overwrite the
 * winner's stored replacement with its own failure state, leaving nothing
 * valid on disk and requiring a browser login to recover.
 *
 * <p>Refresh is therefore <em>single-flight</em>. One lock; whoever arrives
 * first refreshes and everyone else waits and reuses the result. The
 * double-check inside the lock is what makes the waiters cheap - they see the
 * new token and return without issuing a request of their own.
 *
 * <p><strong>2. The replacement must be durable before the old one is spent.</strong>
 * The new refresh token is persisted in its own committed transaction before
 * it is published to callers. If the process dies between the HTTP call and
 * the commit, the previously stored token is still there - stale, but
 * recoverable by a human - rather than the integration holding a token that
 * exists nowhere.
 *
 * <p>The ordering is deliberate and is the reason {@code persistRefreshToken}
 * runs {@code REQUIRES_NEW}: it must not be rolled back by whatever outer
 * transaction happened to trigger the refresh.
 */
public class SchwabAuthService {

    private static final Logger log = LoggerFactory.getLogger(SchwabAuthService.class);

    public static final String PROVIDER = "schwab";

    /** Refresh slightly early so a request never races its own token expiry. */
    private static final Duration EXPIRY_SKEW = Duration.ofMinutes(2);

    private final SchwabOAuthClient oauthClient;
    private final OAuthTokenRepository tokenRepository;
    private final TokenCipher cipher;
    private final Clock clock;

    /** Guards refresh. See the class comment: concurrent refresh strands the account. */
    private final ReentrantLock refreshLock = new ReentrantLock();

    /** The live access token. Volatile so waiters see the winner's result. */
    private volatile TokenPair current;

    /**
     * When the last refresh completed, used to break a refresh loop.
     *
     * If a freshly issued token ever looks already-expired - severe clock skew,
     * or a provider returning a bad expiry - the naive logic would refresh
     * again immediately, and keep doing so on every call. With one-time-use
     * refresh tokens that is not a busy loop, it is a credential shredder: each
     * iteration spends a token. Failing loudly after one such attempt is far
     * better than silently exhausting the account.
     */
    private volatile Instant lastRefreshAt;

    /** A legitimately-issued 30-minute token never needs replacing this soon. */
    private static final Duration MINIMUM_REFRESH_INTERVAL = Duration.ofSeconds(10);

    public SchwabAuthService(SchwabOAuthClient oauthClient,
                             OAuthTokenRepository tokenRepository,
                             TokenCipher cipher,
                             Clock clock) {
        this.oauthClient = oauthClient;
        this.tokenRepository = tokenRepository;
        this.cipher = cipher;
        this.clock = clock;
    }

    /**
     * A valid access token, refreshing if needed.
     *
     * Safe to call concurrently from every scan thread: at most one refresh
     * request is issued however many callers arrive together.
     */
    public String accessToken() {
        TokenPair snapshot = current;
        Instant now = clock.instant();

        if (snapshot != null && !snapshot.accessTokenExpired(now, EXPIRY_SKEW)) {
            return snapshot.accessToken();
        }

        refreshLock.lock();
        try {
            // Double-check: a refresh may have completed while this thread
            // waited for the lock, in which case there is nothing to do.
            TokenPair afterLock = current;
            if (afterLock != null && !afterLock.accessTokenExpired(clock.instant(), EXPIRY_SKEW)) {
                return afterLock.accessToken();
            }
            return refreshLocked().accessToken();
        } finally {
            refreshLock.unlock();
        }
    }

    /** Caller already holds {@link #refreshLock}. */
    private TokenPair refreshLocked() {
        Instant previousRefresh = lastRefreshAt;
        if (previousRefresh != null
                && Duration.between(previousRefresh, clock.instant()).compareTo(MINIMUM_REFRESH_INTERVAL) < 0) {
            // We refreshed moments ago and are being asked again, which means
            // the token we just obtained already reads as expired. Refreshing
            // once more would spend another one-time token and land here again.
            throw new SchwabAuthException(
                    "A newly issued access token already appears expired. This usually means "
                            + "severe clock skew between this host and the provider. Refusing to "
                            + "refresh again, because each attempt consumes a one-time refresh token.",
                    false);
        }

        String storedRefreshToken = loadRefreshToken()
                .orElseThrow(() -> new SchwabAuthException(
                        "No stored Schwab refresh token; complete the authorization flow first",
                        true));

        TokenPair refreshed;
        try {
            refreshed = oauthClient.refresh(storedRefreshToken);
        } catch (SchwabAuthException e) {
            if (e.isReauthorizationRequired()) {
                log.error("Schwab refresh token is no longer valid. A browser re-authorization "
                        + "is required; scheduled scans will fail until it is completed.");
            }
            throw e;
        }

        // Durable before published. If this throws, the previously stored token
        // is untouched and a human can still recover; publishing first would
        // leave a token held only in memory.
        persistRefreshToken(refreshed);

        current = refreshed;
        lastRefreshAt = clock.instant();
        log.info("Schwab access token refreshed; refresh token valid until {}",
                refreshed.refreshTokenExpiresAt());
        return refreshed;
    }

    /**
     * Stores the replacement refresh token in its own committed transaction,
     * so it survives a rollback of whatever triggered the refresh.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistRefreshToken(TokenPair pair) {
        OAuthToken record = tokenRepository.findByProvider(PROVIDER)
                .orElseGet(() -> {
                    OAuthToken fresh = new OAuthToken();
                    fresh.setProvider(PROVIDER);
                    return fresh;
                });

        record.setRefreshTokenEncrypted(cipher.encrypt(pair.refreshToken()));
        record.setRefreshTokenIssuedAt(pair.refreshTokenIssuedAt());
        record.setAccessTokenExpiresAt(pair.accessTokenExpiresAt());
        record.setUpdatedAt(clock.instant());

        tokenRepository.save(record);
    }

    /**
     * Completes the initial authorization-code exchange. A one-time manual step:
     * there is no way to obtain the first refresh token without a human
     * completing a browser login.
     */
    @Transactional
    public void completeAuthorization(String authorizationCode) {
        TokenPair pair = oauthClient.exchangeAuthorizationCode(authorizationCode);
        persistRefreshToken(pair);
        current = pair;
        log.info("Schwab authorization completed; refresh token valid until {}",
                pair.refreshTokenExpiresAt());
    }

    /**
     * How long until the stored refresh token expires, so its age can be
     * alerted on <em>before</em> it strands a scheduled scan - typically over a
     * weekend, when nobody is watching.
     */
    @Transactional(readOnly = true)
    public Optional<Duration> refreshTokenTimeRemaining() {
        return tokenRepository.findByProvider(PROVIDER)
                .map(record -> Duration.between(
                        clock.instant(),
                        record.getRefreshTokenIssuedAt().plus(TokenPair.REFRESH_TOKEN_LIFETIME)));
    }

    @Transactional(readOnly = true)
    public Optional<String> loadRefreshToken() {
        return tokenRepository.findByProvider(PROVIDER)
                .map(record -> cipher.decrypt(record.getRefreshTokenEncrypted()));
    }
}
