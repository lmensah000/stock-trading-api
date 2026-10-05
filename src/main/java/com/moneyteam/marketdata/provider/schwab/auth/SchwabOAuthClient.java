package com.moneyteam.marketdata.provider.schwab.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

/**
 * The HTTP half of the Schwab OAuth flow.
 *
 * Kept separate from {@link SchwabAuthService} so the locking and persistence
 * rules can be tested without a server, and the wire format can be tested
 * without the concurrency.
 *
 * <p>Credentials come from configuration and are sent as HTTP Basic on the
 * token endpoint, per the OAuth spec. They are never logged; error paths
 * deliberately report status codes rather than response bodies, because a
 * failed token response can echo request parameters back.
 */
public class SchwabOAuthClient {

    private static final Logger log = LoggerFactory.getLogger(SchwabOAuthClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public SchwabOAuthClient(HttpClient httpClient,
                             ObjectMapper objectMapper,
                             Clock clock,
                             String tokenUrl,
                             String clientId,
                             String clientSecret,
                             String redirectUri) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    /** First-time exchange of an authorization code for a token pair. */
    public TokenPair exchangeAuthorizationCode(String code) {
        String body = "grant_type=authorization_code"
                + "&code=" + urlEncode(code)
                + "&redirect_uri=" + urlEncode(redirectUri);
        return post(body, "authorization_code exchange");
    }

    /**
     * Spends the supplied refresh token and returns a replacement.
     *
     * The caller must persist the replacement before treating the old one as
     * gone - this method has no way to undo the consumption.
     */
    public TokenPair refresh(String refreshToken) {
        String body = "grant_type=refresh_token&refresh_token=" + urlEncode(refreshToken);
        return post(body, "token refresh");
    }

    private TokenPair post(String formBody, String what) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", basicAuthHeader())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SchwabAuthException("Interrupted during " + what, false, e);
        } catch (Exception e) {
            throw new SchwabAuthException("Network failure during " + what, false, e);
        }

        if (response.statusCode() == 400 || response.statusCode() == 401) {
            // Schwab returns these when the refresh token is expired, already
            // spent, or revoked. None of them are retryable - they need a human.
            throw new SchwabAuthException(
                    what + " rejected with HTTP " + response.statusCode()
                            + "; the refresh token is expired, already used, or revoked",
                    true);
        }
        if (response.statusCode() / 100 != 2) {
            throw new SchwabAuthException(
                    what + " failed with HTTP " + response.statusCode(), false);
        }

        return parse(response.body(), what);
    }

    private TokenPair parse(String body, String what) {
        try {
            JsonNode json = objectMapper.readTree(body);

            String accessToken = required(json, "access_token", what);
            String refreshToken = required(json, "refresh_token", what);
            long expiresInSeconds = json.path("expires_in").asLong(1800);

            var now = clock.instant();
            return new TokenPair(
                    accessToken,
                    refreshToken,
                    now.plusSeconds(expiresInSeconds),
                    now);
        } catch (SchwabAuthException e) {
            throw e;
        } catch (Exception e) {
            // Do not log the body: a token response contains credentials.
            throw new SchwabAuthException("Could not parse the " + what + " response", false, e);
        }
    }

    private static String required(JsonNode json, String field, String what) {
        JsonNode node = json.get(field);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            throw new SchwabAuthException(
                    "The " + what + " response did not contain " + field, false);
        }
        return node.asText();
    }

    private String basicAuthHeader() {
        String raw = clientId + ":" + clientSecret;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
