CREATE TABLE IF NOT EXISTS stocks (
  stock_ticker VARCHAR(32) NOT NULL,
  stock_name   VARCHAR(255),
  sector       VARCHAR(128),
  market_cap_amount DOUBLE,
  volume       BIGINT,
  sizzle_index DOUBLE,
  ask          DECIMAL(19,4),
  bid          DECIMAL(19,4),
  number_of_shares BIGINT,
  `open`       DECIMAL(19,4),
  `close`      DECIMAL(19,4),
  `last`       DECIMAL(19,4),
  mark_change  DECIMAL(19,4),
  PRIMARY KEY (stock_ticker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
