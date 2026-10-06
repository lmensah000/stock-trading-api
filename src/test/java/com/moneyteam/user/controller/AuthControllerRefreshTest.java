package com.moneyteam.user.controller;

import com.moneyteam.common.security.JwtTokenProvider;
import com.moneyteam.common.security.RefreshTokenRepository;
import com.moneyteam.common.security.RefreshTokenStore;
import com.moneyteam.user.dto.RefreshRequest;
import com.moneyteam.user.dto.TokenResponse;
import com.moneyteam.user.model.User;
import com.moneyteam.user.model.enums.Role;
import com.moneyteam.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * The refresh endpoint, end to end over a real {@link JwtTokenProvider} and a
 * real {@link RefreshTokenStore} backed by an in-memory repository.
 *
 * The provider and store are genuine here rather than mocked, because the thing
 * being tested is that issuing, registering, spending and rejecting line up as
 * one sequence. Mocking the store would assert only that the controller calls
 * it, which is the part that was never in doubt.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthControllerRefreshTest {

    private static final String SECRET = "a-test-secret-that-is-at-least-32-bytes-long";

    @Mock
    private UserService userService;

    @Mock
    private RefreshTokenRepository repository;

    private AuthController controller;
    private JwtTokenProvider tokenProvider;

    /** Rows by jti, standing in for the table. */
    private final Map<String, Row> rows = new HashMap<>();

    private static final class Row {
        final Long userId;
        final java.time.Instant expiresAt;
        java.time.Instant usedAt;
        java.time.Instant revokedAt;

        Row(Long userId, java.time.Instant expiresAt) {
            this.userId = userId;
            this.expiresAt = expiresAt;
        }
    }

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRET, 900_000L, 604_800_000L);

        User user = new User();
        user.setId(9L);
        user.setUserName("ada");
        user.setRole(Role.USER);
        when(userService.getUserById(9L)).thenReturn(user);
        when(userService.authenticateUser(anyString(), anyString())).thenReturn(user);

        // A repository that behaves like the SQL: save inserts, spend is a
        // conditional update that can succeed only once.
        when(repository.save(any())).thenAnswer(i -> {
            com.moneyteam.common.security.RefreshTokenRecord record = i.getArgument(0);
            rows.put(record.getJti(), new Row(record.getUserId(), record.getExpiresAt()));
            return record;
        });
        when(repository.findByJti(anyString())).thenAnswer(i -> {
            Row row = rows.get(i.<String>getArgument(0));
            if (row == null) {
                return Optional.empty();
            }
            var record = new com.moneyteam.common.security.RefreshTokenRecord(
                    i.getArgument(0), row.userId, row.expiresAt.minusSeconds(1), row.expiresAt);
            setField(record, "usedAt", row.usedAt);
            setField(record, "revokedAt", row.revokedAt);
            return Optional.of(record);
        });
        when(repository.spend(anyString(), anyString(), any())).thenAnswer(i -> {
            Row row = rows.get(i.<String>getArgument(0));
            java.time.Instant now = i.getArgument(2);
            if (row == null || row.usedAt != null || row.revokedAt != null
                    || !row.expiresAt.isAfter(now)) {
                return 0;
            }
            row.usedAt = now;
            return 1;
        });
        when(repository.revokeAllForUser(anyLong(), any())).thenAnswer(i -> {
            Long userId = i.getArgument(0);
            java.time.Instant now = i.getArgument(1);
            int revoked = 0;
            for (Row row : rows.values()) {
                if (row.userId.equals(userId) && row.revokedAt == null) {
                    row.revokedAt = now;
                    revoked++;
                }
            }
            return revoked;
        });

        RefreshTokenStore store = new RefreshTokenStore(repository, Clock.systemUTC());
        controller = new AuthController(userService, tokenProvider, store);
    }

    private TokenResponse login() {
        var request = new com.moneyteam.user.dto.LoginRequest();
        request.setUserName("ada");
        request.setPassWord("password");
        return controller.login(request).getBody();
    }

    private TokenResponse refresh(String refreshToken) {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(refreshToken);
        return controller.refresh(request).getBody();
    }

    @Test
    @DisplayName("a refresh token works once")
    void firstRedemptionSucceeds() {
        TokenResponse refreshed = refresh(login().getRefreshToken());

        assertThat(refreshed.getAccessToken()).isNotBlank();
        assertThat(refreshed.getRefreshToken()).isNotBlank();
    }

    @Test
    @DisplayName("the same refresh token does not work a second time")
    void secondRedemptionIsRefused() {
        String original = login().getRefreshToken();

        refresh(original);

        assertThatThrownBy(() -> refresh(original))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    @Test
    @DisplayName("redemption returns a different refresh token than the one spent")
    void rotationIssuesANewToken() {
        String original = login().getRefreshToken();

        String replacement = refresh(original).getRefreshToken();

        assertThat(replacement).isNotEqualTo(original);
        // And the replacement is itself redeemable, so a client can keep going.
        assertThat(refresh(replacement).getRefreshToken()).isNotBlank();
    }

    @Test
    @DisplayName("a replay revokes the chain, so the thief's newer token dies too")
    void replayKillsTheWholeChain() {
        String original = login().getRefreshToken();
        String replacement = refresh(original).getRefreshToken();

        // The attacker holds the old token and redeems it. That is the signal.
        assertThatThrownBy(() -> refresh(original))
                .isInstanceOf(BadCredentialsException.class);

        // The legitimate client's current token is now dead as well. That is
        // intended: the server cannot tell holder from thief, so it ends both
        // and forces a login.
        assertThatThrownBy(() -> refresh(replacement))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("an access token presented to the refresh endpoint is refused")
    void accessTokenIsNotAcceptedAsRefresh() {
        String accessToken = login().getAccessToken();

        assertThatThrownBy(() -> refresh(accessToken))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("a refresh token carrying no jti is refused, so pre-rotation tokens die")
    void tokenWithoutJtiIsRefused() {
        // Signed by this server and of the right type, but issued before
        // rotation existed, so there is nothing to spend.
        String legacy = tokenProvider.generateRefreshToken(9L, "ada", "USER", null);

        assertThatThrownBy(() -> refresh(legacy))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("a tampered refresh token is refused")
    void tamperedTokenIsRefused() {
        String token = login().getRefreshToken();

        assertThatThrownBy(() -> refresh(token.substring(0, token.length() - 2) + "xy"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("logging in registers the refresh token, so it is immediately redeemable")
    void loginRegistersItsToken() {
        TokenResponse issued = login();

        assertThat(rows).hasSize(1);
        assertThat(refresh(issued.getRefreshToken())).isNotNull();
    }

    private static void setField(Object target, String name, Object value) {
        if (value == null) {
            return;
        }
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
