package com.moneyteam.marketdata.provider.schwab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.OptionQuote;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.marketdata.provider.schwab.auth.SchwabAuthService;
import com.moneyteam.marketdata.provider.schwab.client.SchwabApiException;
import com.moneyteam.marketdata.provider.schwab.client.SchwabHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Provider behaviour against a stubbed Schwab.
 *
 * Runs without credentials and without touching a rate-limited API, which is
 * the only way to exercise the error paths - a real key cannot be made to
 * return 429 on demand.
 *
 * Resilience4j annotations are applied by a Spring proxy and are therefore not
 * active here; retry and circuit-breaker configuration is asserted separately
 * at the configuration level. What these tests cover is the request shaping,
 * response mapping, and error classification underneath that policy.
 */
class SchwabMarketDataProviderTest {

    private static final Instant NOW = Instant.parse("2024-03-04T19:00:00Z");

    private WireMockServer server;
    private SchwabMarketDataProvider provider;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(options().dynamicPort());
        server.start();

        SchwabAuthService auth = mock(SchwabAuthService.class);
        when(auth.accessToken()).thenReturn("test-access-token");

        SchwabHttpClient http = new SchwabHttpClient(
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                auth,
                "http://localhost:" + server.port(),
                Duration.ofSeconds(5));

        provider = new SchwabMarketDataProvider(http, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    private static String candles(int count, int intervalMinutes) {
        StringBuilder sb = new StringBuilder("{\"candles\":[");
        long base = Instant.parse("2024-03-04T14:30:00Z").toEpochMilli();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"open\":100.%d,\"high\":101.%d,\"low\":99.%d,\"close\":100.%d,\"volume\":%d,\"datetime\":%d}"
                    .formatted(i, i, i, i, 1000 + i, base + (long) i * intervalMinutes * 60_000L));
        }
        return sb.append("]}").toString();
    }

    @Test
    @DisplayName("5-minute bars are requested at the right frequency and mapped back")
    void fetchesFiveMinuteBars() {
        server.stubFor(get(urlPathEqualTo("/marketdata/v1/pricehistory"))
                .withQueryParam("symbol", equalTo("AAPL"))
                .withQueryParam("frequencyType", equalTo("minute"))
                .withQueryParam("frequency", equalTo("5"))
                .willReturn(okJson(candles(10, 5))));

        List<Bar> bars = provider.getBars("AAPL", Timeframe.M5, 10);

        assertThat(bars).hasSize(10);
        assertThat(bars.get(0).timeframe()).isEqualTo(Timeframe.M5);
        Bar.requireOldestFirst(bars);
    }

    @Test
    @DisplayName("an hourly request fetches 30-minute bars and aggregates them, since Schwab has no 1h")
    void hourlyIsAggregatedFromThirtyMinuteBars() {
        server.stubFor(get(urlPathEqualTo("/marketdata/v1/pricehistory"))
                .withQueryParam("frequency", equalTo("30"))
                .willReturn(okJson(candles(8, 30))));

        List<Bar> hourly = provider.getBars("AAPL", Timeframe.H1, 4);

        // Eight 30-minute bars make four hours.
        assertThat(hourly).hasSize(4);
        assertThat(hourly).allSatisfy(b -> assertThat(b.timeframe()).isEqualTo(Timeframe.H1));

        // And it really did ask for 30-minute bars, not an hourly frequency
        // that the API would reject.
        server.verify(getRequestedFor(urlPathEqualTo("/marketdata/v1/pricehistory"))
                .withQueryParam("frequency", equalTo("30")));
    }

    @Test
    @DisplayName("the option chain maps to quotes with a derived open-interest timestamp")
    void fetchesOptionChain() {
        server.stubFor(get(urlPathEqualTo("/marketdata/v1/chains"))
                .willReturn(okJson("""
                        {"callExpDateMap":{"2024-03-15:11":{"190.0":[
                          {"strikePrice":190.0,"bid":5.10,"ask":5.30,"last":5.20,
                           "totalVolume":4200,"openInterest":1000}]}}}""")));

        List<OptionQuote> chain = provider.getOptionChain("AAPL");

        assertThat(chain).hasSize(1);
        OptionQuote quote = chain.get(0);
        assertThat(quote.volumeToOpenInterestRatio()).isEqualByComparingTo("4.2000");
        // Derived from the previous session close, not stamped as "now".
        assertThat(quote.openInterestAsOf()).isBefore(quote.asOf());
    }

    @Test
    @DisplayName("a 429 surfaces as retryable so the retry policy engages")
    void rateLimitIsRetryable() {
        server.stubFor(get(urlPathEqualTo("/marketdata/v1/pricehistory"))
                .willReturn(aResponse().withStatus(429)));

        assertThatThrownBy(() -> provider.getBars("AAPL", Timeframe.M5, 10))
                .isInstanceOf(SchwabApiException.class)
                .satisfies(e -> {
                    assertThat(((SchwabApiException) e).getStatusCode()).isEqualTo(429);
                    assertThat(((SchwabApiException) e).isRetryable()).isTrue();
                });
    }

    @Test
    @DisplayName("a 500 is retryable; a 404 is not")
    void errorClassification() {
        server.stubFor(get(urlPathEqualTo("/marketdata/v1/pricehistory"))
                .withQueryParam("symbol", equalTo("BOOM"))
                .willReturn(aResponse().withStatus(503)));
        server.stubFor(get(urlPathEqualTo("/marketdata/v1/pricehistory"))
                .withQueryParam("symbol", equalTo("NOPE"))
                .willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> provider.getBars("BOOM", Timeframe.M5, 10))
                .satisfies(e -> assertThat(((SchwabApiException) e).isRetryable()).isTrue());

        assertThatThrownBy(() -> provider.getBars("NOPE", Timeframe.M5, 10))
                .satisfies(e -> assertThat(((SchwabApiException) e).isRetryable())
                        .as("retrying a 404 forever helps nobody")
                        .isFalse());
    }

    @Test
    @DisplayName("every request carries the bearer token")
    void sendsBearerToken() {
        server.stubFor(get(urlPathEqualTo("/marketdata/v1/pricehistory"))
                .willReturn(okJson(candles(3, 5))));

        provider.getBars("AAPL", Timeframe.M5, 3);

        server.verify(getRequestedFor(urlPathEqualTo("/marketdata/v1/pricehistory"))
                .withHeader("Authorization", equalTo("Bearer test-access-token")));
    }

    @Test
    @DisplayName("a short series is returned as-is rather than padded or failed")
    void shallowHistoryIsReturnedShort() {
        // Schwab caps intraday history, so asking for more than it holds comes
        // back short. That must look different from an error.
        server.stubFor(get(urlPathEqualTo("/marketdata/v1/pricehistory"))
                .willReturn(okJson(candles(3, 5))));

        assertThat(provider.getBars("AAPL", Timeframe.M5, 500)).hasSize(3);
    }

    @Test
    @DisplayName("option contract bars return empty rather than a fabricated series")
    void optionBarsAreEmpty() {
        // Schwab does not expose per-contract candles. Empty is already a
        // normal answer for this method and the high-value option rule
        // degrades to underlying-only confirmation.
        assertThat(provider.getOptionBars("AAPL:2024-03-15:CALL:190", Timeframe.M5, 10)).isEmpty();
    }
}
