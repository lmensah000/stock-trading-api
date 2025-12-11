-- ============================================================
--  DATABASE
-- ============================================================
CREATE DATABASE IF NOT EXISTS moneyteamdb
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE moneyteamdb;

-- ============================================================
--  USERS
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_name    VARCHAR(100) NOT NULL UNIQUE,
    pass_word    VARCHAR(255) NOT NULL,
    email        VARCHAR(255) UNIQUE,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ============================================================
--  STOCKS
-- ============================================================
CREATE TABLE IF NOT EXISTS stocks (
    stock_ticker      VARCHAR(32) PRIMARY KEY,
    stock_name        VARCHAR(255),
    sector            VARCHAR(128),
    market_cap_amount DOUBLE,
    volume            BIGINT,
    sizzle_index      DOUBLE,
    ask               DECIMAL(19,4),
    bid               DECIMAL(19,4),
    number_of_shares  BIGINT,
    open_price        DECIMAL(19,4),
    close_price       DECIMAL(19,4),
    last_price        DECIMAL(19,4),
    mark_change       DECIMAL(19,4)
) ENGINE=InnoDB;

-- ============================================================
--  POSITIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS positions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    stock_ticker    VARCHAR(32),
    total_quantity  DOUBLE,
    average_price   DECIMAL(19,4),
    unrealized_pnl  DECIMAL(19,4),

    CONSTRAINT fk_position_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_position_stock
        FOREIGN KEY (stock_ticker) REFERENCES stocks(stock_ticker)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
--  TRADES
-- ============================================================
CREATE TABLE IF NOT EXISTS trades (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    stock_ticker   VARCHAR(32) NOT NULL,
    position_id    BIGINT NULL,
    trade_type     ENUM('BUY','SELL','CALL','PUT') NOT NULL,
    status         ENUM('PENDING','EXECUTED','FAILED','CANCELLED') NOT NULL,
    quantity       DOUBLE,
    price          DECIMAL(19,4),
    execution_date DATETIME,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_trade_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_trade_stock
        FOREIGN KEY (stock_ticker) REFERENCES stocks(stock_ticker)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_trade_position
        FOREIGN KEY (position_id) REFERENCES positions(id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
--  OPTION_TRADE_DETAILS (one-to-one with TRADES)
-- ============================================================
CREATE TABLE IF NOT EXISTS option_trade_details (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    trade_id         BIGINT UNIQUE NOT NULL,
    strike_price     DECIMAL(19,4),
    expiration_date  DATE,
    option_type      ENUM('CALL','PUT'),

    CONSTRAINT fk_option_trade
        FOREIGN KEY (trade_id) REFERENCES trades(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
--  WATCHLISTS
-- ============================================================
CREATE TABLE IF NOT EXISTS watchlists (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    name       VARCHAR(100),

    CONSTRAINT fk_watchlist_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- WATCHLIST ↔ STOCK (Many-to-Many)
-- ============================================================
CREATE TABLE IF NOT EXISTS watchlist_stocks (
    watchlist_id  BIGINT NOT NULL,
    stock_ticker  VARCHAR(32) NOT NULL,
    PRIMARY KEY (watchlist_id, stock_ticker),

    CONSTRAINT fk_ws_watchlist
        FOREIGN KEY (watchlist_id) REFERENCES watchlists(id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_ws_stock
        FOREIGN KEY (stock_ticker) REFERENCES stocks(stock_ticker)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
--  TECHNICAL ANALYSIS
-- ============================================================
CREATE TABLE IF NOT EXISTS technical_analysis (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_ticker   VARCHAR(32) NOT NULL,
    moving_average DOUBLE,
    rsi_value      DOUBLE,
    macd           DOUBLE,
    bollinger_high DOUBLE,
    bollinger_low  DOUBLE,

    CONSTRAINT fk_ta_stock
        FOREIGN KEY (stock_ticker) REFERENCES stocks(stock_ticker)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ============================================================
--  FINANCIAL STATEMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS financial_statements (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_ticker    VARCHAR(32) NOT NULL,
    fiscal_year     INT,
    revenue         DECIMAL(19,4),
    net_income      DECIMAL(19,4),
    eps             DECIMAL(10,4),
    debt_to_equity  DECIMAL(10,4),
    cash_flow       DECIMAL(19,4),

    CONSTRAINT fk_fs_stock
        FOREIGN KEY (stock_ticker) REFERENCES stocks(stock_ticker)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;