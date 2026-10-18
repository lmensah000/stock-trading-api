package com.moneyteam.bot;

import com.moneyteam.bot.broker.BrokerOrder;
import com.moneyteam.bot.broker.BrokerPort;
import com.moneyteam.bot.broker.FillReconciler;
import com.moneyteam.marketdata.model.Timeframe;
import com.moneyteam.strategy.engine.ScanResult;
import com.moneyteam.strategy.engine.StrategyEngine;
import com.moneyteam.strategy.model.Signal;
import com.moneyteam.strategy.rule.PercentMoveRule;
import com.moneyteam.trading.dto.OrderRequestDto;
import com.moneyteam.trading.dto.OrderResponseDto;
import com.moneyteam.trading.model.OrderType;
import com.moneyteam.trading.model.enums.OrderSide;
import com.moneyteam.trading.service.OrderService;
import com.moneyteam.trading.service.PositionSizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * The scheduled loop: scan, decide, place, reconcile.
 *
 * <h2>Off by default</h2>
 * The whole class is conditional on {@code app.bot.enabled=true}. While that is
 * false the bean does not exist, so nothing scans and no order is placed -
 * which is the right default for code that can trade.
 *
 * <h2>Single-threaded, deliberately</h2>
 * {@code StrategyEngine} parallelises evaluation because evaluation is
 * read-only. Acting is not: an exposure ceiling is a global constraint, and two
 * threads checking remaining capacity at the same instant both see room for one
 * more position. So this places orders one at a time, on the scheduler thread.
 *
 * <h2>Market hours first</h2>
 * A closed market costs one cheap clock call rather than a universe scan, a
 * spent rate limit and a run of rejected orders logged overnight.
 */
@Component
@ConditionalOnProperty(name = "app.bot.enabled", havingValue = "true")
public class BotRunner {

    private static final Logger log = LoggerFactory.getLogger(BotRunner.class);

    private final BotIdentity botIdentity;
    private final LiveTradingGate gate;
    private final BrokerPort broker;
    private final FillReconciler reconciler;
    private final OrderService orderService;
    private final StrategyEngine engine;
    private final List<String> universe;
    private final BigDecimal riskPercent;
    private final BigDecimal thresholdPercent;

    public BotRunner(BotIdentity botIdentity,
                     LiveTradingGate gate,
                     BrokerPort broker,
                     FillReconciler reconciler,
                     OrderService orderService,
                     com.moneyteam.marketdata.service.MarketDataProvider marketData,
                     @Value("${app.bot.universe:}") String universeCsv,
                     @Value("${app.bot.risk-percent:1}") BigDecimal riskPercent,
                     @Value("${app.bot.threshold-percent:4}") BigDecimal thresholdPercent) {

        this.botIdentity = botIdentity;
        this.gate = gate;
        this.broker = broker;
        this.reconciler = reconciler;
        this.orderService = orderService;
        this.riskPercent = riskPercent;
        this.thresholdPercent = thresholdPercent;

        this.universe = Arrays.stream(universeCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .toList();

        // Daily only. On Alpaca's free tier historical bars lag by about
        // fifteen minutes, so an intraday loop would be acting on stale data;
        // the intraday timeframes stay backtest-only until there is a SIP
        // subscription.
        this.engine = new StrategyEngine(
                marketData,
                List.of(new PercentMoveRule(
                        thresholdPercent, PercentMoveRule.Baseline.PREVIOUS_CLOSE,
                        Timeframe.D1, 0, false)),
                List.of(Timeframe.D1),
                250,
                2,
                2,
                java.time.Duration.ofSeconds(20));
    }

    /**
     * One pass. Default every 15 minutes; the clock check makes an out-of-hours
     * pass nearly free.
     */
    @Scheduled(fixedDelayString = "${app.bot.scan-interval-ms:900000}",
            initialDelayString = "${app.bot.initial-delay-ms:60000}")
    public void scanAndTrade() {
        if (universe.isEmpty()) {
            log.debug("Bot is enabled but app.bot.universe is empty; nothing to scan");
            return;
        }

        if (!broker.isMarketOpen()) {
            log.debug("Market is closed; skipping this pass on {}", broker.describe());
            return;
        }

        Long userId = botIdentity.ownerUserId();
        BigDecimal equity = gate.paperEquity(userId);

        ScanResult result = engine.scan(universe, equity);
        log.info("Scan of {} ticker(s) on {} produced {} signal(s) in {}ms",
                universe.size(), broker.describe(), result.signals().size(),
                result.elapsed().toMillis());

        for (Signal signal : result.signals()) {
            try {
                act(userId, equity, signal);
            } catch (RuntimeException e) {
                // One ticker's failure must not end the pass. A rejected order
                // is information, not a reason to stop managing the others.
                log.warn("Could not act on {} for {}: {}",
                        signal.type(), signal.ticker(), e.toString());
            }
        }
    }

    /**
     * Turns one signal into one order.
     *
     * The stop is the trigger less the threshold percentage, which is a
     * placeholder and known to be one: the real stop belongs at a structural
     * level - the signal bar's low, or an ATR multiple - and that arrives with
     * the setup detection the strategy actually calls for. It is explicit here
     * so it cannot be mistaken for the finished rule.
     */
    private void act(Long userId, BigDecimal equity, Signal signal) {
        BigDecimal trigger = new BigDecimal(
                signal.evidence().getOrDefault("currentPrice", "0"));
        if (trigger.signum() <= 0) {
            log.warn("Signal for {} carried no usable price; skipping", signal.ticker());
            return;
        }

        BigDecimal stop = trigger
                .subtract(trigger.multiply(thresholdPercent)
                        .divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP));

        long shares = PositionSizer.sharesForRisk(equity, riskPercent, trigger, stop);
        if (shares <= 0) {
            log.info("Risk budget allows no shares of {} at {}; skipping", signal.ticker(), trigger);
            return;
        }

        OrderRequestDto dto = new OrderRequestDto();
        dto.setStockTicker(signal.ticker());
        dto.setQuantity((double) shares);
        dto.setSide(OrderSide.BUY);
        dto.setOrderType(OrderType.MARKET);
        dto.setTargetPrice(trigger);

        // Goes through OrderService, so the live-trading gate and - once it
        // exists - the market-stage exposure ceiling both apply. Calling the
        // broker directly would route around both.
        OrderResponseDto local = orderService.placeOrder(userId, dto);

        String clientOrderId = "tckr-" + UUID.randomUUID();
        BrokerOrder placed = broker.place(
                clientOrderId, signal.ticker(), OrderSide.BUY,
                BigDecimal.valueOf(shares), trigger);

        reconciler.track(placed.brokerOrderId(), local.getId(), userId);
        reconciler.reconcile(userId, placed.brokerOrderId());
    }

    /**
     * Polls tracked orders so partial fills are booked as they complete.
     *
     * Separate from the scan because fills arrive on the market's schedule, not
     * ours: an order placed at the end of one pass would otherwise go unbooked
     * until the next.
     */
    @Scheduled(fixedDelayString = "${app.bot.reconcile-interval-ms:60000}",
            initialDelayString = "${app.bot.initial-delay-ms:60000}")
    public void reconcileOpenOrders() {
        int tracked = reconciler.trackedCount();
        if (tracked == 0) {
            return;
        }

        java.math.BigDecimal booked = reconciler.reconcileAll();
        if (booked.signum() > 0) {
            log.info("Booked {} newly filled share(s) across {} tracked order(s)", booked, tracked);
        }
    }
}
