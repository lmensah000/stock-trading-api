-- ============================================================
--  V2 — strategy alerts and staged trade plans
--
--  Adds the storage behind the signal rules: alerts raised when a condition
--  fires, and the trade plans staged alongside them.
--
--  A trade plan is a proposal. No column here records execution, because
--  nothing in the application converts a plan into an order.
-- ============================================================

-- ============================================================
--  ALERTS
--  dedupe_key identifies the underlying condition rather than one observation
--  of it, so repeated scans of an unchanged condition collapse into a single
--  alert instead of one per scan. The index supports that lookup directly.
-- ============================================================
CREATE TABLE alerts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    signal_type     ENUM('PERCENT_MOVE','UNUSUAL_OPTION_VOLUME','HIGH_VALUE_OPTION') NOT NULL,
    stock_ticker    VARCHAR(32) NOT NULL,
    direction       ENUM('BULLISH','BEARISH'),
    confidence      ENUM('HIGH','MEDIUM','LOW') NOT NULL,
    summary         VARCHAR(512) NOT NULL,
    evidence        TEXT,
    dedupe_key      VARCHAR(255) NOT NULL,
    detected_at     TIMESTAMP(6) NOT NULL,
    acknowledged_at TIMESTAMP(6) NULL,

    KEY idx_alert_user (user_id),
    KEY idx_alert_dedupe (user_id, dedupe_key, detected_at),

    CONSTRAINT fk_alert_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
--  TRADE PLANS
--  shares is derived from equity, risk percent and stop distance, so a wider
--  stop yields a smaller position. It is never a function of conviction.
-- ============================================================
CREATE TABLE trade_plans (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    stock_ticker  VARCHAR(32) NOT NULL,
    trigger_price DECIMAL(19,4) NOT NULL,
    stop_price    DECIMAL(19,4) NOT NULL,
    first_target  DECIMAL(19,4) NOT NULL,
    shares        BIGINT NOT NULL,
    risk_percent  DECIMAL(19,4) NOT NULL,
    rationale     TEXT,
    status        ENUM('PROPOSED','ACCEPTED','REJECTED','EXPIRED') NOT NULL,
    created_at    TIMESTAMP(6) NOT NULL,
    decided_at    TIMESTAMP(6) NULL,

    KEY idx_trade_plan_user (user_id),
    KEY idx_trade_plan_status (user_id, status),

    CONSTRAINT fk_trade_plan_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    -- A plan without risk below the entry has no definable size, so the
    -- database refuses it as well as the sizing code.
    CONSTRAINT ck_trade_plan_stop_below_trigger
        CHECK (stop_price < trigger_price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
--  OPTION QUOTE SNAPSHOTS
--  Two timestamps, deliberately. as_of is when the quote and volume were
--  observed; open_interest_as_of is when the open interest figure was
--  published, which is once daily. Any volume/open-interest ratio therefore
--  divides live volume by a denominator up to a session old, and keeping both
--  columns means an alert can state how stale its denominator was rather than
--  leaving the reader to assume.
-- ============================================================
CREATE TABLE option_quotes (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    underlying           VARCHAR(32) NOT NULL,
    option_type          ENUM('CALL','PUT','CREDIT_SPREAD','DEBIT_SPREAD') NOT NULL,
    strike               DECIMAL(19,4) NOT NULL,
    expiration           DATE NOT NULL,
    bid                  DECIMAL(19,4),
    ask                  DECIMAL(19,4),
    last                 DECIMAL(19,4),
    volume               BIGINT NOT NULL DEFAULT 0,
    open_interest        BIGINT NOT NULL DEFAULT 0,
    as_of                TIMESTAMP(6) NOT NULL,
    open_interest_as_of  TIMESTAMP(6) NOT NULL,

    KEY idx_option_quote_underlying (underlying, as_of),
    KEY idx_option_quote_contract (underlying, expiration, option_type, strike),

    CONSTRAINT fk_option_quote_stock
        FOREIGN KEY (underlying) REFERENCES stocks(stock_ticker)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
