# TCKR

A momentum swing-trading API and bot. Spring Boot 2.6.6, Java 17, MySQL.

See `CLAUDE.md` for the domain rules and invariants, and `PHASES.md` for what is
built and what is next.

## Run it

```bash
cp .env.example .env     # then fill in DB_USER, DB_PASS, MYSQL_ROOT_PASSWORD, JWT_SECRET
docker compose up --build
```

`JWT_SECRET` must be at least 32 bytes or the app refuses to start — it will not
issue tokens it cannot sign safely:

```bash
openssl rand -base64 48
```

The API is on `http://127.0.0.1:8080`, bound to loopback rather than all
interfaces. Readiness is `GET /actuator/health`, the only actuator path exposed.

**What happens on first boot:** Flyway applies everything in `db/migration`, and
`spring.jpa.hibernate.ddl-auto=validate` then reconciles every JPA entity
against the result. A boot that succeeds is therefore proof that the schema and
the entities agree. A boot that fails on validation means they have drifted —
that is the check working, not a bug in the container.

Rebuilding does not wipe the database; MySQL lives in the `mysql-data` volume.
To start completely clean:

```bash
docker compose down -v
```

## Run it without Docker

Needs a local MySQL 8 with the database created. `DB_HOST` and `DB_PORT` default
to `127.0.0.1:3306`.

```bash
DB_USER=... DB_PASS=... JWT_SECRET=... mvn spring-boot:run
```

## Tests

```bash
mvn test
```

**200 tests. With Docker running, all 200 pass.** Without Docker one is skipped:
`SchemaMigrationTest` uses Testcontainers to apply the migrations to a real
MySQL 8.0 and validate every entity against them. It is the authoritative check
on the schema, so a run reporting `199 passed, 1 skipped` has **not** verified
the schema. Prefer the number that says 200.

MySQL rather than H2 is deliberate: H2 does not reproduce MySQL's `ENUM`
columns, foreign-key semantics or dialect behaviour, so it can pass while
production fails.

## Configuration

Everything sensitive comes from the environment; see `.env.example` for the full
list. Nothing has a working default, because a default credential is the one
that reaches production unnoticed.

| Variable | Default | Notes |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` logs SQL **and bind parameters**, which include passwords and PII. Never expose a `dev` instance. |
| `BOT_ENABLED` | `false` | While false, nothing scans and no order is placed. |
| `BOT_OWNER_USERNAME` | — | The account the bot acts as. Must already be registered. |
| `BOT_STARTING_EQUITY` | `100000` | What the paper account is funded with, once. |
| `BOT_UNLOCK_TARGET_EQUITY` | — | Required on first boot with the bot on. Immutable after. |
| `MARKETDATA_PROVIDER` | `in-memory` | `in-memory` (fixtures), `csv` (backtesting), `schwab` (live). |

## The live-trading gate

Real money is unreachable until **two separate things** have happened.

1. **Armed** — paper equity reaches `BOT_UNLOCK_TARGET_EQUITY`. The system
   observes this on its own. The target is recorded on first boot and is
   immutable: a gate whose bar can be lowered on the evening it would have
   stopped a trade is decorative.
2. **Unlocked** — you do this, deliberately:

```bash
curl http://127.0.0.1:8080/api/bot/status -H "Authorization: Bearer $TOKEN"

curl -X POST http://127.0.0.1:8080/api/bot/unlock \
     -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"confirmTargetEquity": 130000}'
```

The confirmation must echo the target exactly. That isn't authentication — you
are already authenticated — it is there so an unlock cannot be a mistake.

`POST /api/bot/relock` returns to paper and needs no confirmation.

Two things worth knowing about the gate:

- **A SELL is never blocked**, in either mode. Blocking a close would trap an
  open position, which is more dangerous than no gate at all.
- **Equity counts open positions at cost, not market value.** An unrealized
  gain cannot arm the gate; only booked money counts. This understates a winning
  open position, so it delays arming and never hastens it.

And one thing it is not: **the gate is not the market-stage exposure ceiling.**
`CLAUDE.md` specifies that ceiling as a hard cap in the order path and it does
not exist yet. Unlocking the gate does not mean the strategy's own risk rules
are being enforced.

## Current limitations

- **No signal comes from real market data.** The Schwab adapter is tested only
  against a stubbed server; the default provider is in-memory.
- **The Schwab credentials in this repository's git history are compromised**
  and have not been rotated. Do not set `MARKETDATA_PROVIDER=schwab` with them.
  Issue new ones at the Schwab developer portal first.
- **Nothing places a real broker order.** There is no live trading path.
- **No market-stage gate.** `CLAUDE.md` specifies the exposure ceiling as a hard
  cap in the order path; it is documented but not yet enforced.
- **No backtest results exist**, so the rule thresholds are untested guesses.
