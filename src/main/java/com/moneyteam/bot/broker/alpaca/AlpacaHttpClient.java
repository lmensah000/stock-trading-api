package com.moneyteam.bot.broker.alpaca;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Authenticated JSON transport for Alpaca.
 *
 * Holds no retry or rate-limiting logic: that is applied by Resilience4j at the
 * layer above, so the policy is configured in one place and this class stays
 * testable against a stub server. Same split as {@code SchwabHttpClient}.
 *
 * <h2>The credentials never reach a log</h2>
 * The key pair goes into headers and nowhere else. Failures report the status
 * code, the method and the path, never the headers - an exception message that
 * echoed the request would put a live key into whatever collects the logs.
 */
public class AlpacaHttpClient {

    private static final String KEY_HEADER = "APCA-API-KEY-ID";
    private static final String SECRET_HEADER = "APCA-API-SECRET-KEY";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String keyId;
    private final String secretKey;
    private final Duration requestTimeout;

    public AlpacaHttpClient(HttpClient httpClient,
                            ObjectMapper objectMapper,
                            String baseUrl,
                            String keyId,
                            String secretKey,
                            Duration requestTimeout) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        // Trailing slashes would produce "//v2/orders", which some gateways
        // treat as a different path.
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.keyId = keyId;
        this.secretKey = secretKey;
        this.requestTimeout = requestTimeout;
    }

    public JsonNode get(String path, String query) {
        String url = baseUrl + path + (query == null || query.isBlank() ? "" : "?" + query);
        return send(HttpRequest.newBuilder(URI.create(url)).GET(), "GET", path);
    }

    public JsonNode post(String path, String jsonBody) {
        return send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody)), "POST", path);
    }

    public JsonNode delete(String path) {
        return send(HttpRequest.newBuilder(URI.create(baseUrl + path)).DELETE(), "DELETE", path);
    }

    private JsonNode send(HttpRequest.Builder builder, String method, String path) {
        HttpRequest request = builder
                .timeout(requestTimeout)
                .header(KEY_HEADER, keyId)
                .header(SECRET_HEADER, secretKey)
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AlpacaApiException("Interrupted calling " + method + " " + path, 0, e);
        } catch (Exception e) {
            throw new AlpacaApiException("Network failure calling " + method + " " + path, 0, e);
        }

        int status = response.statusCode();
        if (status / 100 != 2) {
            throw new AlpacaApiException(describeFailure(method, path, status, response.body()), status);
        }

        // A 204 and an empty body are normal for DELETE.
        if (response.body() == null || response.body().isBlank()) {
            return objectMapper.createObjectNode();
        }

        try {
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new AlpacaApiException(
                    "Could not parse the response from " + method + " " + path, status, e);
        }
    }

    private static String describeFailure(String method, String path, int status, String body) {
        if (status == 401 || status == 403) {
            // The commonest real cause, and one the status code alone does not
            // suggest: paper and live have separate key pairs, so a key sent to
            // the wrong host is rejected as unauthorised.
            return ("Alpaca returned HTTP %d for %s %s. Paper and live use separate key pairs, "
                    + "so check the key matches the configured host.")
                    .formatted(status, method, path);
        }

        // Alpaca returns a JSON message on a 422; it names the actual problem
        // (bad symbol, insufficient buying power) and contains no credential.
        String detail = body == null || body.isBlank() ? "" : " Response: " + truncate(body);
        return "Alpaca returned HTTP %d for %s %s.%s".formatted(status, method, path, detail);
    }

    private static String truncate(String body) {
        String flattened = body.replaceAll("\\s+", " ").trim();
        return flattened.length() <= 300 ? flattened : flattened.substring(0, 300) + "...";
    }
}
