package com.moneyteam.marketdata.provider.alpaca;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.moneyteam.bot.broker.alpaca.AlpacaHttpClient;
import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.model.Timeframe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Bars from Alpaca's data API, against a stub.
 */
class AlpacaMarketDataProviderTest {

    private WireMockServer server;
    private AlpacaMarketDataProvider provider;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(0);
        server.start();
        provider = new AlpacaMarketDataProvider(
                new AlpacaHttpClient(HttpClient.newHttpClient(), new ObjectMapper(),
                        "http://localhost:" + server.port(),
                        "k", "s", Duration.ofSeconds(5)),
                "iex");
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    private void stubBars(String body) {
        server.stubFor(get(urlPathEqualTo("/v2/stocks/AAPL/bars"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    @Test
    @DisplayName("bars parse oldest-first with exact decimal prices")
    void parsesBars() {
        stubBars("""
                {"bars":[
                  {"t":"2026-01-02T05:00:00Z","o":100.00,"h":101.00,"l":99.00,"c":100.50,"v":1000},
                  {"t":"2026-01-05T05:00:00Z","o":100.50,"h":106.00,"l":99.50,"c":105.00,"v":5000}
                ],"symbol":"AAPL"}
                """);

        List<Bar> bars = provider.getBars("aapl", Timeframe.D1, 10);

        assertThat(bars).hasSize(2);
        assertThat(bars.get(0).ticker()).isEqualTo("AAPL");
        assertThat(bars.get(0).openTime()).isEqualTo(Instant.parse("2026-01-02T05:00:00Z"));
        // decimalValue(), not asDouble(): gap detection is an exact inequality
        // and a double would make it depend on representation error.
        assertThat(bars.get(1).close()).isEqualByComparingTo("105.00");
        assertThat(bars.get(1).volume()).isEqualTo(5000L);
        assertThat(bars.get(0).openTime()).isBefore(bars.get(1).openTime());
    }

    @Test
    @DisplayName("the request asks for ascending order and the configured feed")
    void requestCarriesFeedAndSort() {
        stubBars("{\"bars\":[]}");

        provider.getBars("AAPL", Timeframe.D1, 50);

        server.verify(com.github.tomakehurst.wiremock.client.WireMock
                .getRequestedFor(urlPathEqualTo("/v2/stocks/AAPL/bars"))
                .withQueryParam("feed", com.github.tomakehurst.wiremock.client.WireMock.equalTo("iex"))
                .withQueryParam("sort", com.github.tomakehurst.wiremock.client.WireMock.equalTo("asc"))
                .withQueryParam("timeframe", com.github.tomakehurst.wiremock.client.WireMock.equalTo("1Day"))
                .withQueryParam("limit", com.github.tomakehurst.wiremock.client.WireMock.equalTo("50")));
    }

    @Test
    @DisplayName("an out-of-order response is rejected, not silently reordered")
    void rejectsOutOfOrderResponse() {
        // asc was requested, but a provider changing its ordering would
        // otherwise invert every indicator instead of failing loudly.
        stubBars("""
                {"bars":[
                  {"t":"2026-01-05T05:00:00Z","o":100.50,"h":106.00,"l":99.50,"c":105.00,"v":5000},
                  {"t":"2026-01-02T05:00:00Z","o":100.00,"h":101.00,"l":99.00,"c":100.50,"v":1000}
                ]}
                """);

        assertThatThrownBy(() -> provider.getBars("AAPL", Timeframe.D1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("oldest-first");
    }

    @Test
    @DisplayName("an empty or missing bars array is an empty series, not an error")
    void emptyResponseIsEmptySeries() {
        stubBars("{\"bars\":[],\"symbol\":\"AAPL\"}");
        assertThat(provider.getBars("AAPL", Timeframe.D1, 10)).isEmpty();

        stubBars("{\"symbol\":\"AAPL\",\"next_page_token\":null}");
        assertThat(provider.getBars("AAPL", Timeframe.D1, 10)).isEmpty();
    }

    @Test
    @DisplayName("every timeframe maps to an Alpaca string, including a native hour")
    void timeframeMapping() {
        assertThat(AlpacaMarketDataProvider.alpacaTimeframe(Timeframe.M5)).isEqualTo("5Min");
        assertThat(AlpacaMarketDataProvider.alpacaTimeframe(Timeframe.M15)).isEqualTo("15Min");
        // Alpaca has a native hourly bar, unlike Schwab - so BarAggregator's
        // 30-minute pairing is not needed on this provider.
        assertThat(AlpacaMarketDataProvider.alpacaTimeframe(Timeframe.H1)).isEqualTo("1Hour");
        assertThat(AlpacaMarketDataProvider.alpacaTimeframe(Timeframe.D1)).isEqualTo("1Day");
        assertThat(AlpacaMarketDataProvider.alpacaTimeframe(Timeframe.W1)).isEqualTo("1Week");
    }

    @Test
    @DisplayName("option data is reported absent rather than fabricated")
    void noOptionData() {
        // Options are a paid tier. An invented chain would make
        // VolumeOpenInterestRule look validated when it is not.
        assertThat(provider.getOptionChain("AAPL")).isEmpty();
        assertThat(provider.getOptionBars("AAPL240119C00150000", Timeframe.M5, 10)).isEmpty();
    }

    @Test
    @DisplayName("previousClose is the close before the latest daily bar")
    void previousCloseIsSecondToLast() {
        stubBars("""
                {"bars":[
                  {"t":"2026-01-02T05:00:00Z","o":100.00,"h":101.00,"l":99.00,"c":100.50,"v":1000},
                  {"t":"2026-01-05T05:00:00Z","o":100.50,"h":106.00,"l":99.50,"c":105.00,"v":5000}
                ]}
                """);

        assertThat(provider.getPreviousClose("AAPL")).isEqualByComparingTo("100.50");
    }

    @Test
    @DisplayName("a single daily bar means there is no previous close")
    void singleBarHasNoPreviousClose() {
        stubBars("""
                {"bars":[{"t":"2026-01-02T05:00:00Z","o":100.00,"h":101.00,"l":99.00,"c":100.50,"v":1000}]}
                """);

        assertThat(provider.getPreviousClose("AAPL")).isNull();
    }

    @Test
    @DisplayName("a bar whose high is below its low is refused by Bar's own guard")
    void impossibleBarIsRejected() {
        stubBars("""
                {"bars":[{"t":"2026-01-02T05:00:00Z","o":100.00,"h":98.00,"l":99.00,"c":100.00,"v":1}]}
                """);

        assertThatThrownBy(() -> provider.getBars("AAPL", Timeframe.D1, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a lookback of zero still asks for at least one bar")
    void lookbackIsClamped() {
        stubBars("{\"bars\":[]}");

        provider.getBars("AAPL", Timeframe.D1, 0);

        // limit=0 would be rejected by the API; clamping is cheaper than
        // propagating a 422 for a caller's rounding.
        server.verify(com.github.tomakehurst.wiremock.client.WireMock
                .getRequestedFor(urlPathEqualTo("/v2/stocks/AAPL/bars"))
                .withQueryParam("limit", com.github.tomakehurst.wiremock.client.WireMock.equalTo("1")));
    }

    @Test
    @DisplayName("a 401 is recognised as a credential/host mismatch")
    void unauthorisedIsRecognised() {
        server.stubFor(get(urlPathEqualTo("/v2/stocks/AAPL/bars"))
                .willReturn(aResponse().withStatus(401).withBody("{\"message\":\"unauthorized\"}")));

        assertThatThrownBy(() -> provider.getBars("AAPL", Timeframe.D1, 10))
                .isInstanceOf(RuntimeException.class)
                .satisfies(e -> assertThat(
                        AlpacaMarketDataProvider.isCredentialHostMismatch((RuntimeException) e)).isTrue());
    }
}
