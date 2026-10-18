package com.moneyteam.bot.backtest;

import com.moneyteam.marketdata.model.Bar;
import com.moneyteam.marketdata.provider.csv.CsvMarketDataProvider;
import com.moneyteam.marketdata.service.MarketDataProvider;
import com.moneyteam.strategy.rule.PercentMoveRule;
import com.moneyteam.strategy.rule.StrategyRule;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Assembles a {@link BacktestRunner} for a request and runs it.
 *
 * Reads bars through the configured {@link MarketDataProvider}, so the same
 * run works against CSV files today and against a real provider later without
 * changing anything here. When the provider is the CSV one the whole series is
 * replayed; any other provider is limited to the lookback it will return,
 * which is said out loud rather than silently truncating a run.
 */
@Service
public class BacktestService {

    /**
     * Enough bars for a meaningful daily replay from a provider that caps its
     * history. Schwab's intraday window is about ten days, so an intraday
     * backtest against it would be near-pointless - another reason CSV is the
     * intended source.
     */
    private static final int NON_CSV_LOOKBACK = 2_000;

    private final MarketDataProvider marketData;

    public BacktestService(MarketDataProvider marketData) {
        this.marketData = marketData;
    }

    public BacktestReport run(BacktestRequest request) {
        List<Bar> series = loadSeries(request);
        if (series.size() < 2) {
            throw new NoSuchElementException(
                    ("No usable bar history for %s %s. The CSV provider expects "
                            + "<directory>/%s_%s.csv with at least two rows.").formatted(
                            request.getTicker(), request.getTimeframe(),
                            request.getTicker().toUpperCase(), request.getTimeframe()));
        }

        StrategyRule rule = new PercentMoveRule(
                request.getThresholdPercent(),
                PercentMoveRule.Baseline.PREVIOUS_CLOSE,
                request.getTimeframe(),
                0,
                false);

        CostModel costs = new CostModel(request.getSlippageBps(), request.getCommissionPerShare());

        BacktestRunner runner = new BacktestRunner(
                List.of(rule), costs, request.getEquity(),
                request.getRiskPercent(), request.getMinimumTrades());

        return runner.run(request.getTicker().toUpperCase(), request.getTimeframe(), series);
    }

    private List<Bar> loadSeries(BacktestRequest request) {
        if (marketData instanceof CsvMarketDataProvider csv) {
            // Whole file: a backtest wants all the history there is.
            return csv.getAllBars(request.getTicker(), request.getTimeframe());
        }
        return marketData.getBars(request.getTicker(), request.getTimeframe(), NON_CSV_LOOKBACK);
    }
}
