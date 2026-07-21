CREATE TABLE IF NOT EXISTS trades (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT,
  stockTicker  VARCHAR(32) NOT NULL,
  trade_type ENUM('STOCK','OPTION') NOT NULL,
  status     ENUM('PENDING','EXECUTED','FAILED') NOT NULL,
  quantity   DOUBLE,
  price      DECIMAL(19,4),
  execution_date DATETIME,
  PRIMARY KEY (id),
  KEY idx_trade_user   (user_id),
  KEY idx_trade_stockTicker (stockTicker),
  KEY idx_trade_date   (execution_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
