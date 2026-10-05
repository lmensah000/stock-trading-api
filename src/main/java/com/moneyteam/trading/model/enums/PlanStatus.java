package com.moneyteam.trading.model.enums;

/**
 * Lifecycle of a staged trade plan.
 *
 * A plan is a proposal, never an instruction. Nothing in this codebase turns a
 * PROPOSED plan into an order; promoting one is a human decision, and ACCEPTED
 * records that the decision was made, not that anything was executed.
 */
public enum PlanStatus {

    /** Computed by a rule and awaiting a human decision. */
    PROPOSED,

    /** A person approved it. Still does not place an order by itself. */
    ACCEPTED,

    /** A person declined it. */
    REJECTED,

    /** The setup went stale before a decision was made. */
    EXPIRED
}
