package com.moneyteam.trading.model;

import com.moneyteam.trading.model.enums.PlanStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * A proposed trade: what would be bought, where the stop sits, how large the
 * position would be, and why.
 *
 * <h2>A plan is not an order</h2>
 * Nothing in this codebase converts a plan into an order. The plan exists so
 * that trigger, stop, size and first target are all computed <em>before</em> a
 * position is contemplated, and so that the reasoning is recorded at the moment
 * it was formed rather than reconstructed afterwards. Reviewing a losing trade
 * honestly is only possible if what was believed at entry was written down at
 * entry.
 *
 * Size comes from {@code PositionSizer}, so it is a function of risk and stop
 * distance - never of how good the setup looked.
 */
@Entity
@Table(name = "trade_plans", indexes = {
        @Index(name = "idx_trade_plan_user", columnList = "user_id"),
        @Index(name = "idx_trade_plan_status", columnList = "user_id,status")
})
public class TradePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "stock_ticker", nullable = false)
    private String stockTicker;

    @Column(name = "trigger_price", nullable = false)
    private BigDecimal triggerPrice;

    @Column(name = "stop_price", nullable = false)
    private BigDecimal stopPrice;

    @Column(name = "first_target", nullable = false)
    private BigDecimal firstTarget;

    @Column(name = "shares", nullable = false)
    private Long shares;

    @Column(name = "risk_percent", nullable = false)
    private BigDecimal riskPercent;

    /** Why this plan was formed - the signal evidence that justified it. */
    @Lob
    @Column(name = "rationale")
    private String rationale;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PlanStatus status = PlanStatus.PROPOSED;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "decided_at")
    private Instant decidedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStockTicker() {
        return stockTicker;
    }

    public void setStockTicker(String stockTicker) {
        this.stockTicker = stockTicker;
    }

    public BigDecimal getTriggerPrice() {
        return triggerPrice;
    }

    public void setTriggerPrice(BigDecimal triggerPrice) {
        this.triggerPrice = triggerPrice;
    }

    public BigDecimal getStopPrice() {
        return stopPrice;
    }

    /**
     * Note for the exit-management work: once a position is live, its stop must
     * only ever move up, and that needs enforcing structurally rather than by
     * convention. This setter is unguarded because a plan is still a proposal -
     * the constraint belongs on the live position, not here.
     */
    public void setStopPrice(BigDecimal stopPrice) {
        this.stopPrice = stopPrice;
    }

    public BigDecimal getFirstTarget() {
        return firstTarget;
    }

    public void setFirstTarget(BigDecimal firstTarget) {
        this.firstTarget = firstTarget;
    }

    public Long getShares() {
        return shares;
    }

    public void setShares(Long shares) {
        this.shares = shares;
    }

    public BigDecimal getRiskPercent() {
        return riskPercent;
    }

    public void setRiskPercent(BigDecimal riskPercent) {
        this.riskPercent = riskPercent;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public void setStatus(PlanStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }
}
