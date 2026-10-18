package com.moneyteam.bot.broker.alpaca;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.moneyteam.bot.broker.BrokerAccount;
import com.moneyteam.bot.broker.BrokerOrder;
import com.moneyteam.trading.model.enums.OrderSide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The Alpaca adapter, against a stubbed server.
 *
 * Stubbed rather than live for the same reasons the Schwab adapter is: no key
 * is needed, CI cannot exhaust a rate limit, and the failure paths that matter
 * most - a 401 from the wrong host, a 404 on an unknown order - can be
 * provoked on demand, which is exactly what a real account will not do when
 * asked.
 */
class AlpacaBrokerTest {

    private WireMockServer server;
    private AlpacaBroker broker;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(0);
        server.start();

        broker = new AlpacaBroker(
                new AlpacaHttpClient(
                        HttpClient.newHttpClient(), new ObjectMapper(),
                        "http://localhost:" + server.port(),
                        "test-key-id", "test-secret", Duration.ofSeconds(5)),
                "localhost (stub)", true);
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    private static String orderJson(String status, String filledQty, String avgPrice) {
        return """
                {"id":"abc-123","client_order_id":"tckr-1","symbol":"AAPL","side":"buy",
                 "qty":"100","filled_qty":"%s","filled_avg_price":%s,"status":"%s",
                 "submitted_at":"2026-01-05T14:30:00Z"}
                """.formatted(filledQty, avgPrice == null ? "null" : "\"" + avgPrice + "\"", status);
    }

    @Test
    @DisplayName("placing an order sends the credentials as headers and the client order id in the body")
    void placeSendsHeadersAndClientOrderId() {
        server.stubFor(post(urlPathEqualTo("/v2/orders"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(orderJson("accepted", "0", null))));

        BrokerOrder order = broker.place("tckr-1", "aapl", OrderSide.BUY,
                new BigDecimal("100"), new BigDecimal("150.00"));

        server.verify(com.github.tomakehurst.wiremock.client.WireMock
                .postRequestedFor(urlPathEqualTo("/v2/orders"))
                .withHeader("APCA-API-KEY-ID", equalTo("test-key-id"))
                .withHeader("APCA-API-SECRET-KEY", equalTo("test-secret"))
                // The idempotency key has to reach the broker or recovery after
                // a lost response is impossible.
                .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock
                        .containing("\"client_order_id\":\"tckr-1\""))
                .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock
                        .containing("\"symbol\":\"AAPL\"")));

        assertThat(order.brokerOrderId()).isEqualTo("abc-123");
        // Ticker is upper-cased on the way out.
        assertThat(order.ticker()).isEqualTo("AAPL");
        assertThat(order.intendedPrice()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("an accepted-but-unfilled order reads as OPEN, not as rejected")
    void acceptedMapsToOpen() {
        server.stubFor(post(urlPathEqualTo("/v2/orders"))
                .willReturn(aResponse().withStatus(200).withBody(orderJson("accepted", "0", null))));

        BrokerOrder order = broker.place("tckr-1", "AAPL", OrderSide.BUY,
                new BigDecimal("100"), new BigDecimal("150.00"));

        assertThat(order.status()).isEqualTo(BrokerOrder.Status.OPEN);
        assertThat(order.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("an unrecognised status defaults to OPEN so a live order is never abandoned")
    void unknownStatusDefaultsToOpen() {
        // A future API version adding a state must not read as a refusal: that
        // would make the reconciler stop polling an order that is still live.
        server.stubFor(post(urlPathEqualTo("/v2/orders"))
                .willReturn(aResponse().withStatus(200)
                        .withBody(orderJson("some_new_state", "0", null))));

        BrokerOrder order = broker.place("tckr-1", "AAPL", OrderSide.BUY,
                new BigDecimal("100"), new BigDecimal("150.00"));

        assertThat(order.status()).isEqualTo(BrokerOrder.Status.OPEN);
    }

    @Test
    @DisplayName("statuses map onto our vocabulary")
    void statusMapping() {
        server.stubFor(get(urlPathEqualTo("/v2/orders/abc-123"))
                .willReturn(aResponse().withStatus(200)
                        .withBody(orderJson("partially_filled", "30", "150.10"))));

        BrokerOrder partial = broker.getOrder("abc-123").orElseThrow();
        assertThat(partial.status()).isEqualTo(BrokerOrder.Status.PARTIALLY_FILLED);
        assertThat(partial.filledQuantity()).isEqualByComparingTo("30");
        assertThat(partial.averageFillPrice()).isEqualByComparingTo("150.10");

        server.stubFor(get(urlPathEqualTo("/v2/orders/abc-123"))
                .willReturn(aResponse().withStatus(200)
                        .withBody(orderJson("canceled", "0", null))));
        assertThat(broker.getOrder("abc-123").orElseThrow().status())
                .isEqualTo(BrokerOrder.Status.CANCELLED);
    }

    @Test
    @DisplayName("a 401 says the key pair may not match the host")
    void unauthorisedNamesTheLikelyCause() {
        server.stubFor(get(urlPathEqualTo("/v2/account"))
                .willReturn(aResponse().withStatus(401).withBody("{\"message\":\"unauthorized\"}")));

        // Paper and live have separate key pairs, so this is the documented
        // failure when a key reaches the wrong environment - and the status
        // code alone does not suggest it.
        assertThatThrownBy(() -> broker.getAccount())
                .isInstanceOf(AlpacaApiException.class)
                .hasMessageContaining("separate key pairs");
    }

    @Test
    @DisplayName("a 401 is not retryable, a 429 and a 500 are")
    void retryabilityFollowsTheStatus() {
        assertThat(new AlpacaApiException("x", 401).isRetryable()).isFalse();
        assertThat(new AlpacaApiException("x", 422).isRetryable()).isFalse();
        assertThat(new AlpacaApiException("x", 429).isRetryable()).isTrue();
        assertThat(new AlpacaApiException("x", 503).isRetryable()).isTrue();

        assertThat(new AlpacaApiException("x", 401).isCredentialHostMismatch()).isTrue();
        assertThat(new AlpacaApiException("x", 500).isCredentialHostMismatch()).isFalse();
    }

    @Test
    @DisplayName("an unknown order is empty rather than an error")
    void unknownOrderIsEmpty() {
        server.stubFor(get(urlPathEqualTo("/v2/orders/nope"))
                .willReturn(aResponse().withStatus(404).withBody("{\"message\":\"not found\"}")));

        assertThat(broker.getOrder("nope")).isEqualTo(Optional.empty());
    }

    @Test
    @DisplayName("cancelling an already-terminal order succeeds silently")
    void cancellingTerminalOrderIsSilent() {
        server.stubFor(delete(urlPathEqualTo("/v2/orders/abc-123"))
                .willReturn(aResponse().withStatus(422).withBody("{\"message\":\"already filled\"}")));

        // BrokerPort specifies this: the caller wanted the order not working,
        // and it is not working.
        broker.cancel("abc-123");
    }

    @Test
    @DisplayName("account numbers parse from strings, not through double")
    void accountParsesExactDecimals() {
        server.stubFor(get(urlPathEqualTo("/v2/account"))
                .willReturn(aResponse().withStatus(200).withBody("""
                        {"account_number":"PA123","equity":"100000.55","cash":"50000.11",
                         "buying_power":"200001.22","status":"ACTIVE"}
                        """)));

        BrokerAccount account = broker.getAccount();

        // Alpaca sends numbers as JSON strings; decimalValue() on a textual
        // node returns zero, so these must be parsed from the text.
        assertThat(account.equity()).isEqualByComparingTo("100000.55");
        assertThat(account.cash()).isEqualByComparingTo("50000.11");
        assertThat(account.buyingPower()).isEqualByComparingTo("200001.22");
        assertThat(account.paper()).isTrue();
    }

    @Test
    @DisplayName("the clock decides whether the market is open")
    void marketOpenComesFromTheClock() {
        server.stubFor(get(urlPathEqualTo("/v2/clock"))
                .willReturn(aResponse().withStatus(200).withBody("{\"is_open\":true}")));
        assertThat(broker.isMarketOpen()).isTrue();

        server.stubFor(get(urlPathEqualTo("/v2/clock"))
                .willReturn(aResponse().withStatus(200).withBody("{\"is_open\":false}")));
        assertThat(broker.isMarketOpen()).isFalse();
    }

    @Test
    @DisplayName("describe() makes the paper/live distinction obvious")
    void describeStatesTheAccountKind() {
        // This string ends up in logs, and "which account did that order go to"
        // is asked after something has gone wrong.
        assertThat(broker.describe()).contains("PAPER");
    }

    @Test
    @DisplayName("positions parse, including a short")
    void positionsParse() {
        server.stubFor(get(urlPathEqualTo("/v2/positions"))
                .willReturn(aResponse().withStatus(200).withBody("""
                        [{"symbol":"AAPL","qty":"100","avg_entry_price":"150.00",
                          "market_value":"15500.00","unrealized_pl":"500.00"},
                         {"symbol":"TSLA","qty":"-50","avg_entry_price":"200.00",
                          "market_value":"-9900.00","unrealized_pl":"100.00"}]
                        """)));

        var positions = broker.getPositions();

        assertThat(positions).hasSize(2);
        assertThat(positions.get(0).isLong()).isTrue();
        assertThat(positions.get(1).isLong()).isFalse();
        assertThat(positions.get(1).quantity()).isEqualByComparingTo("-50");
    }

    @Test
    @DisplayName("adverse slippage is signed so a positive number always means it went against us")
    void slippageIsSignedByDirection() {
        BrokerOrder bought = new BrokerOrder("1", "c", "AAPL", OrderSide.BUY,
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("150.10"),
                new BigDecimal("150.00"), BrokerOrder.Status.FILLED, null);
        BrokerOrder sold = new BrokerOrder("2", "c", "AAPL", OrderSide.SELL,
                new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("149.90"),
                new BigDecimal("150.00"), BrokerOrder.Status.FILLED, null);

        // Buying above the plan and selling below it are both adverse. Raw
        // difference would make half the measurements read as gains.
        assertThat(bought.adverseSlippage()).isEqualByComparingTo("0.10");
        assertThat(sold.adverseSlippage()).isEqualByComparingTo("0.10");
    }
}
