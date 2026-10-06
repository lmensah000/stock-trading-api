package com.moneyteam.user.controller;

import com.moneyteam.common.security.JwtTokenProvider;
import com.moneyteam.common.security.RefreshTokenStore;
import com.moneyteam.user.dto.LoginRequest;
import com.moneyteam.user.dto.RefreshRequest;
import com.moneyteam.user.dto.TokenResponse;
import com.moneyteam.user.model.User;
import com.moneyteam.user.service.UserService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.Instant;

/**
 * Token-based authentication. Both tokens are returned in the response body and
 * are expected back in the Authorization header, so no cookies are involved.
 *
 * Refresh tokens are single-use. Redeeming one spends it and issues a
 * replacement, so a copy taken from a log or a backup stops working as soon as
 * the legitimate holder refreshes. A second redemption of the same token means
 * two parties hold it, and since the server cannot tell them apart it revokes
 * the user's whole set rather than refusing just that call. See
 * {@link RefreshTokenStore}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    public AuthController(UserService userService,
                          JwtTokenProvider tokenProvider,
                          RefreshTokenStore refreshTokenStore) {
        this.userService = userService;
        this.tokenProvider = tokenProvider;
        this.refreshTokenStore = refreshTokenStore;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.authenticateUser(request.getUserName(), request.getPassWord());
        log.info("Issued tokens for user: {}", user.getUserName());
        return ResponseEntity.ok(issueTokens(user));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        Claims claims = tokenProvider.parseClaims(request.getRefreshToken());
        if (!tokenProvider.isTokenOfType(claims, JwtTokenProvider.TYPE_REFRESH)) {
            // Covers expired, tampered, and access-token-presented-as-refresh.
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String presentedJti = tokenProvider.getTokenId(claims);
        if (presentedJti == null) {
            // Issued before rotation existed. Valid signature, but no record to
            // spend, so it cannot be honoured.
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        // Re-read the user so a role change or deletion takes effect at refresh
        // time rather than persisting for the life of the refresh token.
        User user = userService.getUserById(tokenProvider.getUserId(claims));

        // The replacement jti is minted first so it can be recorded against the
        // token being spent, which makes a replay traceable along the chain
        // back to the login that started it.
        String replacementJti = tokenProvider.newTokenId();
        RefreshTokenStore.Outcome outcome = refreshTokenStore.consume(presentedJti, replacementJti);
        if (outcome != RefreshTokenStore.Outcome.SPENT) {
            // Deliberately the same message and status for every failure. Saying
            // which one would tell an attacker holding a stolen token whether it
            // had been used yet, and whether the theft had been noticed.
            log.warn("Refresh rejected for userId={}: {}", user.getId(), outcome);
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        return ResponseEntity.ok(issueTokens(user, replacementJti));
    }

    private TokenResponse issueTokens(User user) {
        return issueTokens(user, tokenProvider.newTokenId());
    }

    private TokenResponse issueTokens(User user, String refreshJti) {
        String role = user.getRole().name();

        Instant now = Instant.now();
        // Registered before the token is built, so there is no window in which
        // a client could hold a token this server has no record of.
        refreshTokenStore.register(refreshJti, user.getId(), now,
                now.plusMillis(tokenProvider.getRefreshTokenTtlMs()));

        return new TokenResponse(
                tokenProvider.generateAccessToken(user.getId(), user.getUserName(), role),
                tokenProvider.generateRefreshToken(user.getId(), user.getUserName(), role, refreshJti),
                tokenProvider.getAccessTokenTtlMs());
    }
}
