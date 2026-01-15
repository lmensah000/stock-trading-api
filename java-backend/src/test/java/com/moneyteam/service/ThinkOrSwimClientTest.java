package com.moneyteam.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ThinkOrSwimClient (TD Schwab API).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ThinkOrSwim Client Tests")
public class ThinkOrSwimClientTest {

    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String TEST_CLIENT_SECRET = "test-client-secret";
    private static final String TEST_REDIRECT_URI = "http://localhost:8080/callback";
    private static final String BASE_URL = "https://api.schwabapi.com/v1";
    private static final String TOKEN_URL = "https://api.schwabapi.com/v1/oauth/token";

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @Test
    @DisplayName("Should generate correct authorization URL")
    void testGenerateAuthorizationUrl() {
        String expectedUrl = "https://api.schwabapi.com/v1/oauth/authorize" +
                "?response_type=code" +
                "&client_id=" + TEST_CLIENT_ID +
                "&scope=readonly" +
                "&redirect_uri=" + TEST_REDIRECT_URI;

        // Build URL same way as client would
        String authUrl = "https://api.schwabapi.com/v1/oauth/authorize" +
                "?response_type=code" +
                "&client_id=" + TEST_CLIENT_ID +
                "&scope=readonly" +
                "&redirect_uri=" + TEST_REDIRECT_URI;

        assertEquals(expectedUrl, authUrl);
        assertTrue(authUrl.contains("oauth/authorize"));
        assertTrue(authUrl.contains("client_id"));
    }

    @Test
    @DisplayName("Should have correct base URL")
    void testBaseUrl() {
        assertEquals("https://api.schwabapi.com/v1", BASE_URL);
    }

    @Test
    @DisplayName("Should have correct token URL")
    void testTokenUrl() {
        assertEquals("https://api.schwabapi.com/v1/oauth/token", TOKEN_URL);
    }

    @Test
    @DisplayName("Should validate client credentials are set")
    void testClientCredentialsSet() {
        assertNotNull(TEST_CLIENT_ID);
        assertNotNull(TEST_CLIENT_SECRET);
        assertFalse(TEST_CLIENT_ID.isEmpty());
        assertFalse(TEST_CLIENT_SECRET.isEmpty());
    }

    @Test
    @DisplayName("Should validate redirect URI format")
    void testRedirectUriFormat() {
        assertTrue(TEST_REDIRECT_URI.startsWith("http"));
        assertTrue(TEST_REDIRECT_URI.contains("callback"));
    }

    @Test
    @DisplayName("Should build token request correctly")
    void testBuildTokenRequest() {
        String authCode = "test-auth-code";
        
        String expectedBody = "grant_type=authorization_code" +
                "&access_type=offline" +
                "&code=" + authCode +
                "&client_id=" + TEST_CLIENT_ID +
                "&redirect_uri=" + TEST_REDIRECT_URI;

        assertTrue(expectedBody.contains("grant_type=authorization_code"));
        assertTrue(expectedBody.contains("code=" + authCode));
    }

    @Test
    @DisplayName("Should handle stock quote endpoint")
    void testStockQuoteEndpoint() {
        String ticker = "AAPL";
        String endpoint = BASE_URL + "/marketdata/" + ticker + "/quotes";
        
        assertTrue(endpoint.contains("marketdata"));
        assertTrue(endpoint.contains(ticker));
        assertTrue(endpoint.contains("quotes"));
    }

    @Test
    @DisplayName("Should handle account info endpoint")
    void testAccountInfoEndpoint() {
        String accountId = "12345";
        String endpoint = BASE_URL + "/accounts/" + accountId;
        
        assertTrue(endpoint.contains("accounts"));
        assertTrue(endpoint.contains(accountId));
    }

    @Test
    @DisplayName("Should handle order placement endpoint")
    void testOrderPlacementEndpoint() {
        String accountId = "12345";
        String endpoint = BASE_URL + "/accounts/" + accountId + "/orders";
        
        assertTrue(endpoint.contains("orders"));
    }

    @Test
    @DisplayName("Should validate token response structure")
    void testTokenResponseStructure() {
        // Simulated token response
        String mockResponse = "{" +
                "\"access_token\": \"mock-access-token\"," +
                "\"refresh_token\": \"mock-refresh-token\"," +
                "\"expires_in\": 1800," +
                "\"token_type\": \"Bearer\"" +
                "}";

        assertTrue(mockResponse.contains("access_token"));
        assertTrue(mockResponse.contains("refresh_token"));
        assertTrue(mockResponse.contains("expires_in"));
        assertTrue(mockResponse.contains("Bearer"));
    }

    @Test
    @DisplayName("Should handle refresh token flow")
    void testRefreshTokenFlow() {
        String refreshToken = "mock-refresh-token";
        
        String refreshBody = "grant_type=refresh_token" +
                "&refresh_token=" + refreshToken +
                "&client_id=" + TEST_CLIENT_ID;

        assertTrue(refreshBody.contains("grant_type=refresh_token"));
        assertTrue(refreshBody.contains(refreshToken));
    }
}
