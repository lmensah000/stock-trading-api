package com.moneyteam.marketdata.provider.schwab.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneyteam.marketdata.provider.schwab.auth.SchwabAuthService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Thin authenticated GET wrapper over the Schwab market-data endpoints.
 *
 * Holds no retry or rate-limiting logic of its own - that is applied by
 * Resilience4j at the provider layer, so the policy is configured in one place
 * and this class stays trivially testable against a stub server.
 */
public class SchwabHttpClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SchwabAuthService authService;
    private final String baseUrl;
    private final Duration requestTimeout;

    public SchwabHttpClient(HttpClient httpClient,
                            ObjectMapper objectMapper,
                            SchwabAuthService authService,
                            String baseUrl,
                            Duration requestTimeout) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.authService = authService;
        this.baseUrl = baseUrl;
        this.requestTimeout = requestTimeout;
    }

    /**
     * @param path  path beginning with "/", e.g. "/marketdata/v1/pricehistory"
     * @param query already-encoded query string without the leading "?"
     */
    public JsonNode get(String path, String query) {
        String url = baseUrl + path + (query == null || query.isBlank() ? "" : "?" + query);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(requestTimeout)
                // Resolved per call: the token may have been refreshed by
                // another thread since this request was queued.
                .header("Authorization", "Bearer " + authService.accessToken())
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SchwabApiException("Interrupted calling " + path, 0, e);
        } catch (Exception e) {
            throw new SchwabApiException("Network failure calling " + path, 0, e);
        }

        if (response.statusCode() / 100 != 2) {
            throw new SchwabApiException(
                    "Schwab returned HTTP " + response.statusCode() + " for " + path,
                    response.statusCode());
        }

        try {
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new SchwabApiException("Could not parse the response from " + path, 0, e);
        }
    }
}
