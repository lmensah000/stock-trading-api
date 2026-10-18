package com.moneyteam.bot.backtest;

import com.moneyteam.marketdata.model.Timeframe;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Parameters for one backtest run.
 *
 * No user id: the acting user comes from the authenticated principal, as
 * everywhere else. Defaults are filled from configuration when a field is
 * omitted, so a minimal request is just a ticker.
 */
public class BacktestRequest {

    @NotBlank
    private String ticker;

    @NotNull
    private Timeframe timeframe = Timeframe.D1;

    /** Move that must be met for the percentage rule to fire, e.g. 4 for 4%. */
    @Positive
    private BigDecimal thresholdPercent = new BigDecimal("4");

    /** Equity the run is sized against. Not the real account balance. */
    @Positive
    private BigDecimal equity = new BigDecimal("100000");

    @Positive
    private BigDecimal riskPercent = BigDecimal.ONE;

    @PositiveOrZero
    private BigDecimal slippageBps = new BigDecimal("10");

    @PositiveOrZero
    private BigDecimal commissionPerShare = BigDecimal.ZERO;

    /** Closed trades required before win rate and expectancy are reported. */
    @Positive
    private Integer minimumTrades = BacktestReport.DEFAULT_MINIMUM_TRADES;

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public Timeframe getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(Timeframe timeframe) {
        this.timeframe = timeframe;
    }

    public BigDecimal getThresholdPercent() {
        return thresholdPercent;
    }

    public void setThresholdPercent(BigDecimal thresholdPercent) {
        this.thresholdPercent = thresholdPercent;
    }

    public BigDecimal getEquity() {
        return equity;
    }

    public void setEquity(BigDecimal equity) {
        this.equity = equity;
    }

    public BigDecimal getRiskPercent() {
        return riskPercent;
    }

    public void setRiskPercent(BigDecimal riskPercent) {
        this.riskPercent = riskPercent;
    }

    public BigDecimal getSlippageBps() {
        return slippageBps;
    }

    public void setSlippageBps(BigDecimal slippageBps) {
        this.slippageBps = slippageBps;
    }

    public BigDecimal getCommissionPerShare() {
        return commissionPerShare;
    }

    public void setCommissionPerShare(BigDecimal commissionPerShare) {
        this.commissionPerShare = commissionPerShare;
    }

    public Integer getMinimumTrades() {
        return minimumTrades;
    }

    public void setMinimumTrades(Integer minimumTrades) {
        this.minimumTrades = minimumTrades;
    }
}
