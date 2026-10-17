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

**182 tests. With Docker running, all 182 pass.** Without Docker one is skipped:
`SchemaMigrationTest` uses Testcontainers to apply the migrations to a real
MySQL 8.0 and validate every entity against them. It is the authoritative check
on the schema, so a run reporting `181 passed, 1 skipped` has **not** verified
the schema. Prefer the number that says 182.

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
| `MARKETDATA_PROVIDER` | `in-memory` | `in-memory` (fixtures), `csv` (backtesting), `schwab` (live). |

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
