package com.moneyteam.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Single-use refresh tokens, and what happens when one is presented twice.
 *
 * The behaviour worth protecting is that redemption is not idempotent. A
 * refresh token that can be spent twice is a credential with a seven-day life
 * and no way to retire it early, so these tests treat the second redemption,
 * not the first, as the interesting case.
 *
 * Note on scope: the atomicity of the spend is a property of the SQL, not of
 * this class - it rests on InnoDB taking a row lock for a conditional UPDATE.
 * That cannot be demonstrated against a mock, and the migration test that
 * would exercise it needs Docker. What is tested here is that this class
 * handles losing that race correctly, which is the half that lives in Java.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenStoreTest {

    private static final Instant NOW = Instant.parse("2024-03-04T19:00:00Z");
    private static final long USER = 42L;
    private static final String JTI = "11111111-1111-1111-1111-111111111111";
    private static final String NEXT_JTI = "22222222-2222-2222-2222-222222222222";

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenStore store;

    @BeforeEach
    void setUp() {
        store = new RefreshTokenStore(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static RefreshTokenRecord live() {
        return new RefreshTokenRecord(JTI, USER, NOW.minusSeconds(60), NOW.plusSeconds(3600));
    }

    @Test
    @DisplayName("an unspent token is spent, and the replacement is recorded against it")
    void spendsAnUnspentToken() {
        when(repository.findByJti(JTI)).thenReturn(Optional.of(live()));
        when(repository.spend(JTI, NEXT_JTI, NOW)).thenReturn(1);

        assertThat(store.consume(JTI, NEXT_JTI)).isEqualTo(RefreshTokenStore.Outcome.SPENT);
    }

    @Test
    @DisplayName("presenting an already-spent token revokes everything the user holds")
    void replayRevokesTheFamily() {
        RefreshTokenRecord spent = live();
        // Simulate the row as the database would return it after a redemption.
        when(repository.findByJti(JTI)).thenReturn(Optional.of(spent));
        when(repository.spend(JTI, NEXT_JTI, NOW)).thenReturn(0);
        when(repository.revokeAllForUser(USER, NOW)).thenReturn(3);

        assertThat(store.consume(JTI, NEXT_JTI)).isEqualTo(RefreshTokenStore.Outcome.REPLAYED);

        // The whole point: refusing the one call would leave the thief's other
        // tokens live. A replay means the token leaked, so the set goes.
        verify(repository).revokeAllForUser(USER, NOW);
    }

    @Test
    @DisplayName("losing the race to a concurrent redemption is treated as a replay")
    void concurrentRedemptionYieldsExactlyOneWinner() {
        // The conditional UPDATE is the gate, so model it as succeeding once.
        AtomicBoolean alreadySpent = new AtomicBoolean(false);
        when(repository.findByJti(JTI)).thenReturn(Optional.of(live()));
        when(repository.spend(eq(JTI), anyString(), eq(NOW)))
                .thenAnswer(i -> alreadySpent.compareAndSet(false, true) ? 1 : 0);
        when(repository.revokeAllForUser(anyLong(), any())).thenReturn(1);

        RefreshTokenStore.Outcome first = store.consume(JTI, NEXT_JTI);
        RefreshTokenStore.Outcome second = store.consume(JTI, "33333333-3333-3333-3333-333333333333");

        assertThat(first).isEqualTo(RefreshTokenStore.Outcome.SPENT);
        assertThat(second).isEqualTo(RefreshTokenStore.Outcome.REPLAYED);
    }

    @Test
    @DisplayName("a token from an already-revoked family is refused without re-revoking")
    void revokedTokenIsRefused() {
        RefreshTokenRecord record = live();
        setField(record, "revokedAt", NOW.minusSeconds(10));
        when(repository.findByJti(JTI)).thenReturn(Optional.of(record));

        assertThat(store.consume(JTI, NEXT_JTI)).isEqualTo(RefreshTokenStore.Outcome.REVOKED);

        verify(repository, never()).spend(anyString(), anyString(), any());
        verify(repository, never()).revokeAllForUser(anyLong(), any());
    }

    @Test
    @DisplayName("an expired token is refused, and is not treated as a security event")
    void expiredTokenIsNotAReplay() {
        RefreshTokenRecord expired =
                new RefreshTokenRecord(JTI, USER, NOW.minusSeconds(7200), NOW.minusSeconds(1));
        when(repository.findByJti(JTI)).thenReturn(Optional.of(expired));

        assertThat(store.consume(JTI, NEXT_JTI)).isEqualTo(RefreshTokenStore.Outcome.EXPIRED);

        // Expiry is ordinary. Revoking the family on it would log every user
        // out whenever they left a tab open over a weekend.
        verify(repository, never()).revokeAllForUser(anyLong(), any());
    }

    @Test
    @DisplayName("a token expiring exactly now is already too late")
    void expiryBoundaryIsExclusive() {
        RefreshTokenRecord onTheBoundary =
                new RefreshTokenRecord(JTI, USER, NOW.minusSeconds(3600), NOW);
        when(repository.findByJti(JTI)).thenReturn(Optional.of(onTheBoundary));

        assertThat(store.consume(JTI, NEXT_JTI)).isEqualTo(RefreshTokenStore.Outcome.EXPIRED);
    }

    @Test
    @DisplayName("a correctly signed token with no stored record is refused")
    void unknownTokenIsRefused() {
        when(repository.findByJti(JTI)).thenReturn(Optional.empty());

        assertThat(store.consume(JTI, NEXT_JTI)).isEqualTo(RefreshTokenStore.Outcome.UNKNOWN);

        verify(repository, never()).spend(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("registering stores the jti, the owner and the expiry")
    void registerStoresTheRecord() {
        store.register(JTI, USER, NOW, NOW.plusSeconds(3600));

        org.mockito.ArgumentCaptor<RefreshTokenRecord> captor =
                org.mockito.ArgumentCaptor.forClass(RefreshTokenRecord.class);
        verify(repository).save(captor.capture());

        RefreshTokenRecord saved = captor.getValue();
        assertThat(saved.getJti()).isEqualTo(JTI);
        assertThat(saved.getUserId()).isEqualTo(USER);
        assertThat(saved.getExpiresAt()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(saved.isUsed()).isFalse();
        assertThat(saved.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("the purge deletes rows whose tokens have already expired")
    void purgeUsesNowAsTheCutoff() {
        when(repository.deleteExpiredBefore(NOW)).thenReturn(7);

        assertThat(store.purgeExpired()).isEqualTo(7);

        verify(repository).deleteExpiredBefore(NOW);
    }

    @Test
    @DisplayName("the stored record never holds the token itself, only its id")
    void recordHoldsNoCredential() {
        // A jti is a random id with no authority; the signature is what proves
        // the token genuine. Storing the token would put a live credential in
        // the database for no gain.
        assertThat(java.util.Arrays.stream(RefreshTokenRecord.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName))
                .doesNotContain("token", "refreshToken", "tokenValue");

        assertThat(live().toString()).doesNotContain("eyJ");
    }

    private static void setField(Object target, String name, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
