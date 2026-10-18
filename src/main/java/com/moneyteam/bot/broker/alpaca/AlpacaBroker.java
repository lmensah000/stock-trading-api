package com.moneyteam.bot.broker.alpaca;

import com.fasterxml.jackson.databind.JsonNode;
import com.moneyteam.bot.broker.BrokerAccount;
import com.moneyteam.bot.broker.BrokerOrder;
import com.moneyteam.bot.broker.BrokerPort;
import com.moneyteam.bot.broker.BrokerPosition;
import com.moneyteam.trading.model.enums.OrderSide;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Alpaca, against the paper host only.
 *
 * <h2>Paper only, and structurally so</h2>
 * The base URL is injected and the configuration supplies
 * {@code paper-api.alpaca.markets}. There is no live URL anywhere in this
 * codebase, not even commented out. That matters more than it looks: Alpaca
 * issues separate key pairs for paper and live, so a paper key is rejected by
 * the live host with a 401. The gate against trading real money is therefore
 * enforced by Alpaca's infrastructure as well as by {@code LiveTradingGate} -
 * two independent mechanisms, neither relying on the other being correct.
 *
 * Unlocking the gate grants no live execution path today. Building one is a
 * separate, deliberate piece of work.
 *
 * <h2>Resilience</h2>
 * Rate limiting, retry and circuit breaking are declared here and configured in
 * {@code application.properties}, as with the Schwab adapter. The free tier
 * allows 200 requests a minute, so the limiter is load-bearing: a universe scan
 * would otherwise convert throughput into 429s.
 *
 * Only {@link #place} is deliberately <strong>not</strong> retried. Retrying an
 * order whose response was lost is how one intended position becomes two. The
 * client order id makes recovery possible instead - see {@link BrokerPort#place}.
 */
public class AlpacaBroker implements BrokerPort {

    private static final Logger log = LoggerFactory.getLogger(AlpacaBroker.class);

    private static final String ORDERS_PATH = "/v2/orders";
    private static final String ACCOUNT_PATH = "/v2/account";
    private static final String POSITIONS_PATH = "/v2/positions";
    private static final String CLOCK_PATH = "/v2/clock";

    private final AlpacaHttpClient http;
    private final String hostDescription;
    private final boolean paper;

    /**
     * @param hostDescription the host, for logs. "did that order go to the
     *        paper account or the real one" is asked after something has gone
     *        wrong, when the configuration may already have changed.
     * @param paper whether this points at the paper host. Passed in rather than
     *        sniffed from the URL so a renamed host cannot silently flip an
     *        account from simulated to real.
     */
    public AlpacaBroker(AlpacaHttpClient http, String hostDescription, boolean paper) {
        this.http = http;
        this.hostDescription = hostDescription;
        this.paper = paper;
    }

    /**
     * Submits a market order, day time-in-force.
     *
     * No {@code @Retry}: see the class note. A timeout here leaves the order's
     * fate unknown, and the correct recovery is to look it up by client order
     * id, not to send it again.
     */
    @Override
    @RateLimiter(name = "alpacaBroker")
    @CircuitBreaker(name = "alpacaBroker")
    public BrokerOrder place(String clientOrderId,
                             String ticker,
                             OrderSide side,
                             BigDecimal quantity,
                             BigDecimal intendedPrice) {

        String body = """
                {"symbol":"%s","qty":"%s","side":"%s","type":"market",\
                "time_in_force":"day","client_order_id":"%s"}"""
                .formatted(ticker.toUpperCase(),
                        quantity.stripTrailingZeros().toPlainString(),
                        side == OrderSide.BUY ? "buy" : "sell",
                        clientOrderId);

        log.info("Placing {} {} x {} on {} (clientOrderId={})",
                side, quantity, ticker, hostDescription, clientOrderId);

        return toOrder(http.post(ORDERS_PATH, body), intendedPrice);
    }

    @Override
    @RateLimiter(name = "alpacaBroker")
    @Retry(name = "alpacaBroker")
    @CircuitBreaker(name = "alpacaBroker")
    public Optional<BrokerOrder> getOrder(String brokerOrderId) {
        try {
            return Optional.of(toOrder(http.get(ORDERS_PATH + "/" + brokerOrderId, null), null));
        } catch (AlpacaApiException e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    @RateLimiter(name = "alpacaBroker")
    @Retry(name = "alpacaBroker")
    @CircuitBreaker(name = "alpacaBroker")
    public void cancel(String brokerOrderId) {
        try {
            http.delete(ORDERS_PATH + "/" + brokerOrderId);
        } catch (AlpacaApiException e) {
            // 404 means it is already gone and 422 that it is already terminal.
            // Both mean "not cancellable", which is the state the caller wanted.
            if (e.statusCode() == 404 || e.statusCode() == 422) {
                log.debug("Order {} was already terminal when cancellation was attempted", brokerOrderId);
                return;
            }
            throw e;
        }
    }

    @Override
    @RateLimiter(name = "alpacaBroker")
    @Retry(name = "alpacaBroker")
    @CircuitBreaker(name = "alpacaBroker")
    public List<BrokerPosition> getPositions() {
        JsonNode array = http.get(POSITIONS_PATH, null);
        List<BrokerPosition> positions = new ArrayList<>();
        if (array != null && array.isArray()) {
            for (JsonNode node : array) {
                positions.add(new BrokerPosition(
                        node.path("symbol").asText(),
                        decimal(node, "qty"),
                        decimal(node, "avg_entry_price"),
                        decimal(node, "market_value"),
                        decimal(node, "unrealized_pl")));
            }
        }
        return List.copyOf(positions);
    }

    @Override
    @RateLimiter(name = "alpacaBroker")
    @Retry(name = "alpacaBroker")
    @CircuitBreaker(name = "alpacaBroker")
    public BrokerAccount getAccount() {
        JsonNode node = http.get(ACCOUNT_PATH, null);
        return new BrokerAccount(
                node.path("account_number").asText(null),
                decimal(node, "equity"),
                decimal(node, "cash"),
                decimal(node, "buying_power"),
                paper);
    }

    @Override
    @RateLimiter(name = "alpacaBroker")
    @Retry(name = "alpacaBroker")
    @CircuitBreaker(name = "alpacaBroker")
    public boolean isMarketOpen() {
        return http.get(CLOCK_PATH, null).path("is_open").asBoolean(false);
    }

    @Override
    public String describe() {
        return "Alpaca[" + (paper ? "PAPER" : "LIVE") + " " + hostDescription + "]";
    }

    /**
     * Maps Alpaca's status vocabulary onto ours.
     *
     * Alpaca has more states than we model - {@code accepted},
     * {@code pending_new}, {@code held}, {@code calculated} - and all of them
     * mean "accepted, nothing filled yet" for our purposes. The default is
     * {@code OPEN} rather than {@code REJECTED} so an unrecognised new state in
     * a future API version does not read as a refusal, which would make the
     * reconciler abandon a live order.
     */
    private static BrokerOrder.Status status(String alpacaStatus, BigDecimal filled, BigDecimal quantity) {
        String s = alpacaStatus == null ? "" : alpacaStatus.toLowerCase();
        return switch (s) {
            case "filled" -> BrokerOrder.Status.FILLED;
            case "partially_filled" -> BrokerOrder.Status.PARTIALLY_FILLED;
            case "canceled", "cancelled", "expired", "done_for_day" -> BrokerOrder.Status.CANCELLED;
            case "rejected", "suspended" -> BrokerOrder.Status.REJECTED;
            default -> filled != null && filled.signum() > 0
                    ? BrokerOrder.Status.PARTIALLY_FILLED
                    : BrokerOrder.Status.OPEN;
        };
    }

    private static BrokerOrder toOrder(JsonNode node, BigDecimal intendedPrice) {
        BigDecimal quantity = decimal(node, "qty");
        BigDecimal filled = decimal(node, "filled_qty");

        return new BrokerOrder(
                node.path("id").asText(null),
                node.path("client_order_id").asText(null),
                node.path("symbol").asText(null),
                "sell".equalsIgnoreCase(node.path("side").asText()) ? OrderSide.SELL : OrderSide.BUY,
                quantity,
                filled,
                nullableDecimal(node, "filled_avg_price"),
                intendedPrice,
                status(node.path("status").asText(null), filled, quantity),
                instant(node, "submitted_at"));
    }

    /**
     * Parses a numeric field. Alpaca sends numbers as JSON strings, and
     * {@code decimalValue()} on a textual node returns zero, so the text is
     * parsed explicitly - a silent zero quantity would be far worse than a
     * parse failure.
     */
    private static BigDecimal decimal(JsonNode node, String field) {
        BigDecimal value = nullableDecimal(node, field);
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal nullableDecimal(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        if (text == null || text.isBlank() || "null".equals(text)) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new AlpacaApiException("Could not parse '" + field + "' as a number: " + text, 0, e);
        }
    }

    private static Instant instant(JsonNode node, String field) {
        String text = node.path(field).asText(null);
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(text);
        } catch (RuntimeException e) {
            // A timestamp we cannot read is not worth failing an order over.
            return null;
        }
    }
}
