CREATE TABLE IF NOT EXISTS watchlist_stocks (
  watchlist_id BIGINT NOT NULL,
  stock_stockTicker VARCHAR(32) NOT NULL,
  PRIMARY KEY (watchlist_id, stock_stockTicker),
  CONSTRAINT fk_watchlist
    FOREIGN KEY (watchlist_id) REFERENCES watchlists(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_stock
    FOREIGN KEY (stock_stockTicker) REFERENCES stocks(stock_ticker)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
