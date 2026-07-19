CREATE TABLE IF NOT EXISTS positions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT,
  stockTicker  VARCHAR(32),
  total_quantity DOUBLE,
  average_price  DECIMAL(19,4),
  unrealized_pnl DECIMAL(19,4),
  PRIMARY KEY (id),
  KEY idx_pos_user   (user_id),
  KEY idx_pos_stockTicker (stockTicker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;