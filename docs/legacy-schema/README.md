# Legacy schema files (superseded by Flyway V1)

These files are kept for reference only. **They are not used by the
application** and must not be applied to a database. `src/main/resources/db/migration/V1__baseline_schema.sql`
is the sole source of schema truth, and Hibernate validates against it.

They are preserved because they record the schema's prior shape, which is
useful when interpreting an older database dump.

## What was here, and why it went

The repository carried **three conflicting definitions** of the same schema,
which disagreed with each other and with the JPA entities:

- `tableSchema.sql` — the fullest definition: every table, in snake_case.
- `positions.sql`, `trades.sql`, `stock.sql`, `watchlists.sql`,
  `watchlists_stocks.sql` — per-table definitions that contradicted
  `tableSchema.sql`.

Specific contradictions:

| File | Declared | Conflict |
|---|---|---|
| `positions.sql`, `trades.sql` | `stockTicker` | `tableSchema.sql` said `stock_ticker` |
| `trades.sql` | `trade_type ENUM('STOCK','OPTION')` | That enum was retired; trades carry `side ENUM('BUY','SELL')` |
| `watchlists_stocks.sql` | `stock_stockTicker` | `tableSchema.sql` said `stock_ticker` |
| `stock.sql` | `volume BIGINT` | `Stock.volume` is an `Integer` |
| `watchlists.sql` | no `name` column | `tableSchema.sql` declared one |

Because `ddl-auto` was disabled, none of this was ever checked at startup, so
the drift accumulated silently. V1 resolves it in favour of the entities, with
column naming standardised on snake_case.
