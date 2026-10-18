package com.moneyteam.bot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneyteam.bot.broker.BrokerPort;
import com.moneyteam.bot.broker.FillReconciler;
import com.moneyteam.bot.broker.InMemoryBroker;
import com.moneyteam.bot.broker.alpaca.AlpacaBroker;
import com.moneyteam.bot.broker.alpaca.AlpacaHttpClient;
import com.moneyteam.trading.service.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

/**
 * Chooses the broker.
 *
 * Defaults to the in-memory one, so a fresh checkout and the test suite run
 * with no account anywhere and nothing can reach a market by accident.
 *
 * <h2>Only Alpaca's paper host is configurable</h2>
 * There is deliberately no property for a live Alpaca URL and no live key pair,
 * not even commented out. Alpaca issues separate credentials per environment,
 * so a paper key is refused by the live host - which means real money is out of
 * reach for two independent reasons: no configuration points at it, and the
 * credentials we hold would not work there.
 *
 * Unlocking {@code LiveTradingGate} does not change that. A live adapter is a
 * separate piece of work and should stay that way until there is a reason to
 * build it.
 */
@Configuration
public class BrokerConfig {

    /** Alpaca's paper host. Hardcoded, not a property: see the class note. */
    private static final String PAPER_BASE_URL = "https://paper-api.alpaca.markets";

    // ---------------------------------------------------------------
    //  In-memory (default)
    // ---------------------------------------------------------------

    @Bean
    @ConditionalOnProperty(name = "app.bot.broker", havingValue = "in-memory", matchIfMissing = true)
    public BrokerPort inMemoryBroker(
            Clock clock,
            @Value("${app.bot.starting-equity:100000}") BigDecimal startingEquity) {
        return new InMemoryBroker(clock, startingEquity);
    }

    // ---------------------------------------------------------------
    //  Alpaca paper
    // ---------------------------------------------------------------

    @Bean
    @ConditionalOnProperty(name = "app.bot.broker", havingValue = "alpaca-paper")
    public AlpacaHttpClient alpacaTradingHttpClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            @Value("${app.bot.alpaca.key-id}") String keyId,
            @Value("${app.bot.alpaca.secret-key}") String secretKey,
            @Value("${app.bot.alpaca.request-timeout-seconds:20}") long timeoutSeconds) {

        // No defaults for the credentials: startup fails rather than silently
        // running with blanks, matching the rule that a default credential is
        // worse than a missing one.
        requirePresent(keyId, "app.bot.alpaca.key-id (APCA_API_KEY_ID)");
        requirePresent(secretKey, "app.bot.alpaca.secret-key (APCA_API_SECRET_KEY)");

        return new AlpacaHttpClient(httpClient, objectMapper, PAPER_BASE_URL,
                keyId, secretKey, Duration.ofSeconds(timeoutSeconds));
    }

    @Bean
    @ConditionalOnProperty(name = "app.bot.broker", havingValue = "alpaca-paper")
    public BrokerPort alpacaBroker(AlpacaHttpClient alpacaTradingHttpClient) {
        return new AlpacaBroker(alpacaTradingHttpClient, PAPER_BASE_URL, true);
    }

    // ---------------------------------------------------------------
    //  Reconciliation
    // ---------------------------------------------------------------

    @Bean
    public FillReconciler fillReconciler(BrokerPort broker, OrderService orderService) {
        return new FillReconciler(broker, orderService);
    }

    private static void requirePresent(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    name + " must be set when app.bot.broker=alpaca-paper. "
                            + "Use PAPER keys only - never live keys.");
        }
    }
}
