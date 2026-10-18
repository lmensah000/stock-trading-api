package com.moneyteam.marketdata.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneyteam.bot.broker.alpaca.AlpacaHttpClient;
import com.moneyteam.marketdata.provider.alpaca.AlpacaMarketDataProvider;
import com.moneyteam.marketdata.provider.csv.CsvMarketDataProvider;
import com.moneyteam.marketdata.provider.schwab.SchwabMarketDataProvider;
import com.moneyteam.marketdata.provider.schwab.auth.OAuthTokenRepository;
import com.moneyteam.marketdata.provider.schwab.auth.SchwabAuthService;
import com.moneyteam.marketdata.provider.schwab.auth.SchwabOAuthClient;
import com.moneyteam.marketdata.provider.schwab.auth.TokenCipher;
import com.moneyteam.marketdata.provider.schwab.client.SchwabHttpClient;
import com.moneyteam.marketdata.service.MarketDataProvider;
import com.moneyteam.marketdata.service.impl.InMemoryMarketDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;

/**
 * Chooses the market-data provider.
 *
 * Defaults to the in-memory adapter so tests and a fresh checkout run without
 * credentials. Selecting {@code schwab} requires every credential to be present
 * in the environment; there are deliberately no defaults, because a default
 * credential is worse than a missing one.
 *
 * The injected {@link Clock} comes from {@code common/config/CoreConfig}; it is
 * shared infrastructure rather than a market-data concern.
 */
@Configuration
public class MarketDataConfig {

    @Bean
    public HttpClient marketDataHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                // Schwab's token endpoint rejects some redirect handling, and
                // a market-data API should not be redirecting anyway.
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    // ---------------------------------------------------------------
    //  In-memory (default)
    // ---------------------------------------------------------------

    @Bean
    @ConditionalOnProperty(name = "app.marketdata.provider", havingValue = "in-memory", matchIfMissing = true)
    public MarketDataProvider inMemoryMarketDataProvider() {
        return new InMemoryMarketDataProvider();
    }

    // ---------------------------------------------------------------
    //  CSV (backtesting)
    // ---------------------------------------------------------------

    /**
     * Bars from files on disk. The one provider that needs no credentials,
     * which is why backtesting is reachable while the Schwab rotation is
     * still outstanding.
     */
    @Bean
    @ConditionalOnProperty(name = "app.marketdata.provider", havingValue = "csv")
    public MarketDataProvider csvMarketDataProvider(
            @Value("${app.marketdata.csv.directory:data}") String directory) {
        return new CsvMarketDataProvider(Path.of(directory));
    }

    // ---------------------------------------------------------------
    //  Alpaca
    // ---------------------------------------------------------------

    /**
     * Bars from Alpaca's data API, using its own host and its own credentials.
     *
     * Separate from the trading client in {@code BrokerConfig} because the two
     * are different hosts: data lives on data.alpaca.markets while orders go to
     * paper-api.alpaca.markets. The same key pair authenticates both.
     *
     * The free tier is IEX-only and delayed about fifteen minutes - usable for
     * prices and daily bars, not for calibrating anything volume-based. See
     * {@link AlpacaMarketDataProvider}.
     */
    @Bean
    @ConditionalOnProperty(name = "app.marketdata.provider", havingValue = "alpaca")
    public MarketDataProvider alpacaMarketDataProvider(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            @Value("${app.bot.alpaca.key-id:}") String keyId,
            @Value("${app.bot.alpaca.secret-key:}") String secretKey,
            @Value("${app.marketdata.alpaca.base-url:https://data.alpaca.markets}") String baseUrl,
            @Value("${app.marketdata.alpaca.feed:iex}") String feed,
            @Value("${app.marketdata.alpaca.request-timeout-seconds:20}") long timeoutSeconds) {

        if (keyId == null || keyId.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "app.marketdata.provider=alpaca requires app.bot.alpaca.key-id and "
                            + "app.bot.alpaca.secret-key (APCA_API_KEY_ID / APCA_API_SECRET_KEY). "
                            + "Use PAPER keys.");
        }

        return new AlpacaMarketDataProvider(
                new AlpacaHttpClient(httpClient, objectMapper, baseUrl, keyId, secretKey,
                        Duration.ofSeconds(timeoutSeconds)),
                feed);
    }

    // ---------------------------------------------------------------
    //  Schwab
    // ---------------------------------------------------------------

    @Bean
    @ConditionalOnProperty(name = "app.marketdata.provider", havingValue = "schwab")
    public SchwabOAuthClient schwabOAuthClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${app.marketdata.schwab.token-url}") String tokenUrl,
            @Value("${app.marketdata.schwab.client-id}") String clientId,
            @Value("${app.marketdata.schwab.client-secret}") String clientSecret,
            @Value("${app.marketdata.schwab.redirect-uri}") String redirectUri) {

        return new SchwabOAuthClient(httpClient, objectMapper, clock, tokenUrl,
                clientId, clientSecret, redirectUri);
    }

    @Bean
    @ConditionalOnProperty(name = "app.marketdata.provider", havingValue = "schwab")
    public SchwabAuthService schwabAuthService(SchwabOAuthClient oauthClient,
                                               OAuthTokenRepository tokenRepository,
                                               TokenCipher cipher,
                                               Clock clock) {
        return new SchwabAuthService(oauthClient, tokenRepository, cipher, clock);
    }

    @Bean
    @ConditionalOnProperty(name = "app.marketdata.provider", havingValue = "schwab")
    public SchwabHttpClient schwabHttpClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            SchwabAuthService authService,
            @Value("${app.marketdata.schwab.base-url}") String baseUrl,
            @Value("${app.marketdata.schwab.request-timeout-seconds:20}") long timeoutSeconds) {

        return new SchwabHttpClient(httpClient, objectMapper, authService, baseUrl,
                Duration.ofSeconds(timeoutSeconds));
    }

    @Bean
    @ConditionalOnProperty(name = "app.marketdata.provider", havingValue = "schwab")
    public MarketDataProvider schwabMarketDataProvider(SchwabHttpClient client, Clock clock) {
        return new SchwabMarketDataProvider(client, clock);
    }
}
