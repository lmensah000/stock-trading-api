-- ============================================================
--  V1 — baseline schema
--
--  Authored to match the JPA entities, which are the source of truth.
--  This replaces the previously hand-maintained tableSchema.sql (kept at
--  docs/legacy-schema.sql for reference). That file had drifted from the
--  entities: it declared stock_ticker where several entities mapped the
--  literal column "stockTicker", and number_of_shares against an entity
--  mapping of number_of_share. Those mappings were corrected alongside
--  this migration, so column names here are snake_case throughout.
--
--  The database itself is created out of band; Flyway manages only its
--  contents. Charset/collation are set per-table for MySQL 8.
-- ============================================================

-- ============================================================
--  USERS
-- ============================================================
CREATE TABLE users (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_name    VARCHAR(100) NOT NULL UNIQUE,
    pass_word    VARCHAR(255) NOT NULL,
    email        VARCHAR(255) UNIQUE,
    role         ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
    created_at   TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
--  STOCKS
--  Raw quote fields stay DOUBLE: they are market data, not money.
--  A value becomes money once multiplied by a quantity or settled,
--  and that arithmetic happens in BigDecimal on trades/positions.
-- ============================================================
CREATE TABLE stocks (
    stock_ticker      VARCHAR(32) PRIMARY KEY,
    stock_name        VARCHAR(255),
    sector            VARCHAR(128),
    market_cap_amount DOUBLE,
    volume            INT,
    sizzle_index      DOUBLE,
    ask               DOUBLE,
    bid               DOUBLE,
    number_of_shares  INT,
    open_price        DOUBLE,
    close_price       DOUBLE,
    last_price        DOUBLE,
    mark_change       DOUBLE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- @ElementCollection on Stock.historicalData
CREATE TABLE stock_historical_data (
    stock_ticker VARCHAR(32) NOT NULL,
    price        DOUBLE,

    KEY idx_shd_stock (stock_ticker),
    CONSTRAINT fk_shd_stock
        FOREIGN KEY (stock_ticker) REFERENCES stocks(stock_ticker)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
--  ACCOUNTS
-- ============================================================
CREATE TABLE accounts (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT NOT NULL UNIQUE,
    cash_balance   DECIMAL(19,4) NOT NULL DEFAULT 0,
    buying_power   DECIMAL(19,4) NOT NULL DEFAULT 0,

    CONSTRAINT fk_account_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
--  POSITIONS
-- ============================================================
CREATE TABLE positions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    stock_ticker    VARCHAR(32),
    total_quantity  DOUBLE,
    average_price   DECIMAL(19,4),
    unrealized_pnl  DECIMAL(19,4),
    realized_pnl    DECIMAL(19,4) DEFAULT 0,

    KEY idx_position_user (user_id),
    KEY idx_position_stock_ticker (stock_ticker),

    CONSTRAINT fk_position_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_position_stock
        FOREIGN KEY (stock_ticker) REFERENCES stocks(stock_ticker)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
--  TRADES
-- ============================================================
CREATE TABLE trades (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    stock_ticker   VARCHAR(32) NOT NULL,
    position_id    BIGINT NULL,
    side           ENUM('BUY','SELL') NOT NULL,
    status         ENUM('PENDING','EXECUTED','FAILED','CANCELLED') NOT NULL,
    quantity       DOUBLE,
    price          DECIMAL(19,4),
    execution_date DATETIME,
    created_at     TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,

    KEY idx_trade_user (user_id),
    KEY idx_trade_stock_ticker (stock_ticker),
    KEY idx_trade_date (execution_date),

    CONSTRAINT fk_trade_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_trade_stock
        FOREIGN KEY (stock_ticker) REFERENCES stocks(stock_ticker)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_trade_position
        FOREIGN KEY (position_id) REFERENCES positions(id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
--  LEDGER ENTRIES
-- ============================================================
CREATE TABLE ledger_entries (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id       BIGINT NOT NULL,
    type             ENUM('DEPOSIT','WITHDRAWAL','TRADE_SETTLEMENT','FEE','DIVIDEND') NOT NULL,
    amount           DECIMAL(19,4) NOT NULL,
    timestamp        TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    related_trade_id BIGINT NULL,

    KEY idx_ledger_account (account_id),

    CONSTRAINT fk_ledger_account
        FOREIGN KEY (account_id) REFERENCES accounts(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
--  ORDERS
--  target_price is DECIMAL: a limit price is an order value, i.e. money.
-- ============================================================
CREATE TABLE orders (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    stock_ticker   VARCHAR(32) NOT NULL,
    quantity       DOUBLE,
    target_price   DECIMAL(19,4),
    order_type     ENUM('MARKET','LIMIT','STOP') NOT NULL,
    side           ENUM('BUY','SELL') NOT NULL,
    status         ENUM('NEW','PENDING','PARTIALLY_FILLED','FILLED','CANCELLED','REJECTED','EXPIRED') NOT NULL,
    created_at     TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,

    KEY idx_order_user (user_id),
    KEY idx_order_ticker (stock_ticker),

    CONSTRAINT fk_order_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_order_stock
        FOREIGN KEY (stock_ticker) REFERENCES stocks(stock_ticker)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
--  ORDER EXECUTIONS (partial/full fills)
-- ============================================================
CREATE TABLE order_executions (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id         BIGINT NOT NULL,
    filled_quantity  DOUBLE NOT NULL,
    fill_price       DECIMAL(19,4) NOT NULL,
    executed_at      TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,

    KEY idx_execution_order (order_id),

    CONSTRAINT fk_execution_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
--  OPTIONS
-- ============================================================
CREATE TABLE options (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_ticker  VARCHAR(32),
    option_type   ENUM('CALL','PUT'),
    strike_price  DECIMAL(19,4),
    expiration    VARCHAR(64),
    user_id       BIGINT NULL,

    KEY idx_options_stock (stock_ticker),
    KEY idx_options_user (user_id),

    CONSTRAINT fk_options_stock
        FOREIGN KEY (stock_ticker) REFERENCES stocks(stock_ticker)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_options_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
--  OPTION TRADE DETAILS (one-to-one with TRADES)
--  strike_price is DECIMAL: an order value, matching Options.strikePrice.
-- ============================================================
CREATE TABLE option_trade_details (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    trade_id         BIGINT NOT NULL UNIQUE,
    strike_price     DECIMAL(19,4),
    expiration_date  DATE,
    option_type      VARCHAR(16),

    CONSTRAINT fk_option_trade
        FOREIGN KEY (trade_id) REFERENCES trades(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
--  WATCHLISTS
-- ============================================================
CREATE TABLE watchlists (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,

    KEY idx_watchlist_user (user_id),

    CONSTRAINT fk_watchlist_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE watchlist_stocks (
    watchlist_id  BIGINT NOT NULL,
    stock_ticker  VARCHAR(32) NOT NULL,
    PRIMARY KEY (watchlist_id, stock_ticker),

    CONSTRAINT fk_ws_watchlist
        FOREIGN KEY (watchlist_id) REFERENCES watchlists(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ws_stock
        FOREIGN KEY (stock_ticker) REFERENCES stocks(stock_ticker)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
