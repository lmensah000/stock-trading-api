package com.moneyteam.user.controller;

import com.moneyteam.common.security.JwtTokenProvider;
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

/**
 * Token-based authentication. Both tokens are returned in the response body and
 * are expected back in the Authorization header, so no cookies are involved.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;

    public AuthController(UserService userService, JwtTokenProvider tokenProvider) {
        this.userService = userService;
        this.tokenProvider = tokenProvider;
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

        // Re-read the user so a role change or deletion takes effect at refresh
        // time rather than persisting for the life of the refresh token.
        User user = userService.getUserById(tokenProvider.getUserId(claims));
        return ResponseEntity.ok(issueTokens(user));
    }

    private TokenResponse issueTokens(User user) {
        String role = user.getRole().name();
        return new TokenResponse(
                tokenProvider.generateAccessToken(user.getId(), user.getUserName(), role),
                tokenProvider.generateRefreshToken(user.getId(), user.getUserName(), role),
                tokenProvider.getAccessTokenTtlMs());
    }
}
