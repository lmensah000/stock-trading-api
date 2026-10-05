package com.moneyteam.marketdata.provider.schwab.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Token lifecycle behaviour.
 *
 * The concurrency test here is the most important test in this change. Schwab
 * refresh tokens are one-time use, so a refresh stampede does not merely waste
 * calls - it races several threads to spend the same token, and the losers can
 * leave nothing valid stored, which strands the integration until a human
 * completes a browser login.
 */
class SchwabAuthServiceTest {

    private static final Instant T0 = Instant.parse("2024-03-04T14:00:00Z");

    /**
     * A stateful stub of the token row, built with Mockito rather than by hand:
     * implementing JpaRepository directly ties the test to whichever methods
     * the current Spring Data version happens to declare.
     */
    private static final class TokenStore {
        private OAuthToken stored;
        final AtomicInteger saves = new AtomicInteger();

        OAuthTokenRepository mock() {
            OAuthTokenRepository repository = org.mockito.Mockito.mock(OAuthTokenRepository.class);
            org.mockito.Mockito.when(repository.findByProvider(org.mockito.ArgumentMatchers.anyString()))
                    .thenAnswer(i -> Optional.ofNullable(stored));
            org.mockito.Mockito.when(repository.save(org.mockito.ArgumentMatchers.any(OAuthToken.class)))
                    .thenAnswer(i -> {
                        saves.incrementAndGet();
                        stored = i.getArgument(0);
                        return stored;
                    });
            return repository;
        }

        OAuthToken stored() {
            return stored;
        }
    }

    /** Counts refreshes and can be made slow, so a stampede has a window to happen in. */
    private static final class CountingOAuthClient extends SchwabOAuthClient {
        final AtomicInteger refreshCalls = new AtomicInteger();
        final AtomicReference<String> lastTokenSeen = new AtomicReference<>();
        private final Duration delay;
        private volatile boolean rejectAsExpired;
        /** Tokens are issued relative to this, as a real provider issues them relative to now. */
        private volatile Instant issuedAt = T0;

        CountingOAuthClient(Duration delay) {
            super(null, null, null, "", "", "", "");
            this.delay = delay;
        }

        void issuingFrom(Instant now) {
            this.issuedAt = now;
        }

        @Override
        public TokenPair refresh(String refreshToken) {
            refreshCalls.incrementAndGet();
            lastTokenSeen.set(refreshToken);

            if (rejectAsExpired) {
                throw new SchwabAuthException("refresh token already spent", true);
            }
            sleep(delay);

            int n = refreshCalls.get();
            return new TokenPair("access-" + n, "refresh-" + n, issuedAt.plusSeconds(1800), issuedAt);
        }

        @Override
        public TokenPair exchangeAuthorizationCode(String code) {
            return new TokenPair("access-initial", "refresh-initial", T0.plusSeconds(1800), T0);
        }

        private static void sleep(Duration d) {
            try {
                Thread.sleep(d.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private TokenStore tokenStore;
    private OAuthTokenRepository repository;
    private TokenCipher cipher;

    @BeforeEach
    void setUp() {
        tokenStore = new TokenStore();
        repository = tokenStore.mock();
        // A fixed 32-byte key; this is test-only and encrypts nothing real.
        cipher = new TokenCipher(java.util.Base64.getEncoder()
                .encodeToString("0123456789abcdef0123456789abcdef".getBytes()));
    }

    private SchwabAuthService serviceWith(CountingOAuthClient client, Clock clock) {
        SchwabAuthService service = new SchwabAuthService(client, repository, cipher, clock);
        service.completeAuthorization("initial-code");
        // A real provider issues tokens valid from now; the initial exchange
        // above deliberately uses T0 so the token starts stale where a test
        // wants to force a refresh.
        client.issuingFrom(clock.instant());
        return service;
    }

    @Test
    @DisplayName("concurrent expiry triggers exactly ONE refresh, not one per thread")
    void refreshIsSingleFlight() throws Exception {
        // A slow refresh widens the window so a stampede would actually occur.
        CountingOAuthClient client = new CountingOAuthClient(Duration.ofMillis(100));

        // A clock far past the initial token's expiry: every caller sees it stale.
        Clock expired = Clock.fixed(T0.plusSeconds(7200), ZoneOffset.UTC);
        SchwabAuthService service = serviceWith(client, expired);

        int threads = 24;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startTogether = new CountDownLatch(1);
        List<String> tokens = java.util.Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                startTogether.await();
                tokens.add(service.accessToken());
                return null;
            });
        }
        startTogether.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // The assertion that matters: one refresh, however many threads arrived.
        assertThat(client.refreshCalls.get())
                .as("a one-time-use refresh token must be spent exactly once")
                .isEqualTo(1);

        // And every caller got the same token, rather than some getting a
        // token that a competing refresh had already invalidated.
        assertThat(tokens).hasSize(threads);
        assertThat(tokens).containsOnly(tokens.get(0));
    }

    @Test
    @DisplayName("a valid access token is reused without touching the network")
    void validTokenIsNotRefreshed() {
        CountingOAuthClient client = new CountingOAuthClient(Duration.ZERO);
        Clock fresh = Clock.fixed(T0.plusSeconds(60), ZoneOffset.UTC);
        SchwabAuthService service = serviceWith(client, fresh);

        for (int i = 0; i < 10; i++) {
            service.accessToken();
        }

        assertThat(client.refreshCalls.get()).isZero();
    }

    @Test
    @DisplayName("the replacement refresh token is persisted, so a restart can recover")
    void replacementTokenIsPersisted() {
        CountingOAuthClient client = new CountingOAuthClient(Duration.ZERO);
        Clock expired = Clock.fixed(T0.plusSeconds(7200), ZoneOffset.UTC);
        SchwabAuthService service = serviceWith(client, expired);

        int savesAfterAuthorization = tokenStore.saves.get();
        service.accessToken();

        assertThat(tokenStore.saves.get())
                .as("the refresh wrote the replacement token")
                .isGreaterThan(savesAfterAuthorization);
        assertThat(service.loadRefreshToken()).contains("refresh-1");
    }

    @Test
    @DisplayName("the refresh spends the stored token, not a stale in-memory copy")
    void refreshUsesStoredToken() {
        CountingOAuthClient client = new CountingOAuthClient(Duration.ZERO);
        Clock expired = Clock.fixed(T0.plusSeconds(7200), ZoneOffset.UTC);
        SchwabAuthService service = serviceWith(client, expired);

        service.accessToken();

        assertThat(client.lastTokenSeen.get()).isEqualTo("refresh-initial");
    }

    @Test
    @DisplayName("an expired refresh token surfaces as needing re-authorization, not a generic error")
    void expiredRefreshTokenIsDistinguishable() {
        CountingOAuthClient client = new CountingOAuthClient(Duration.ZERO);
        Clock expired = Clock.fixed(T0.plusSeconds(7200), ZoneOffset.UTC);
        SchwabAuthService service = serviceWith(client, expired);
        client.rejectAsExpired = true;

        assertThatThrownBy(service::accessToken)
                .isInstanceOf(SchwabAuthException.class)
                .satisfies(e -> assertThat(((SchwabAuthException) e).isReauthorizationRequired())
                        .as("a spent token needs a human, and retrying it forever will not help")
                        .isTrue());
    }

    @Test
    @DisplayName("remaining refresh-token life is reported, so expiry can be alerted before it strands a scan")
    void reportsRefreshTokenLifetime() {
        CountingOAuthClient client = new CountingOAuthClient(Duration.ZERO);
        // Six days after issue: one day left of the seven-day window.
        Clock sixDaysLater = Clock.fixed(T0.plus(Duration.ofDays(6)), ZoneOffset.UTC);
        SchwabAuthService service = serviceWith(client, sixDaysLater);

        assertThat(service.refreshTokenTimeRemaining())
                .isPresent()
                .get()
                .satisfies(remaining -> assertThat(remaining.toHours()).isEqualTo(24));
    }

    @Test
    @DisplayName("a token born expired fails loudly instead of refreshing in a loop")
    void refuseToRefreshInALoop() {
        // Simulates severe clock skew: the provider keeps issuing tokens that
        // this host already considers expired. Without a guard the service
        // would refresh on every call, and because each refresh consumes a
        // one-time token, it would shred the credential within seconds.
        CountingOAuthClient client = new CountingOAuthClient(Duration.ZERO);
        Clock skewed = Clock.fixed(T0.plusSeconds(7200), ZoneOffset.UTC);
        SchwabAuthService service = new SchwabAuthService(client, repository, cipher, skewed);
        service.completeAuthorization("initial-code");
        // Leave the client issuing tokens stamped at T0, i.e. already stale.

        service.accessToken();   // first refresh is legitimate

        assertThatThrownBy(service::accessToken)
                .isInstanceOf(SchwabAuthException.class)
                .hasMessageContaining("clock skew");

        assertThat(client.refreshCalls.get())
                .as("exactly one token was spent before giving up")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("the stored refresh token is encrypted, not plaintext")
    void storedTokenIsEncrypted() {
        CountingOAuthClient client = new CountingOAuthClient(Duration.ZERO);
        serviceWith(client, Clock.fixed(T0, ZoneOffset.UTC));

        byte[] stored = tokenStore.stored().getRefreshTokenEncrypted();

        assertThat(new String(stored))
                .as("a database leak must not hand over a working credential")
                .doesNotContain("refresh-initial");
    }
}
