# TCKR

A momentum swing-trading API and bot. Spring Boot 2.6.6, Java 17, MySQL.

See `CLAUDE.md` for the domain rules and invariants, and `PHASES.md` for what is
built and what is next.

## See it work

```bash
./scripts/demo.sh
```

Walks the analytical core end to end - bars, signals, gaps, sizing, backtest,
broker, and the journal's articulation-gap analysis - using the real production
classes. Needs no database, no credentials and no Docker. The bar series is
generated, so its R figures show the machinery works and are **not** a backtest
result; the runner says so in its own output.

`docs/DEMO.md` has that plus the full `docker compose` walkthrough with curl
commands.

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

**333 tests. With Docker running, all 333 pass.** Without Docker one is skipped:
`SchemaMigrationTest` uses Testcontainers to apply the migrations to a real
MySQL 8.0 and validate every entity against them. It is the authoritative check
on the schema, so a run reporting `332 passed, 1 skipped` has **not** verified
the schema. Prefer the number that says 333.

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
| `MARKETDATA_PROVIDER` | `in-memory` | `in-memory` (fixtures), `csv` (backtesting), `alpaca` (IEX), `schwab` (live). |
| `BOT_BROKER` | `in-memory` | `in-memory` or `alpaca-paper`. No live option exists. |
| `MARKETDATA_CSV_DIR` | `data` | Where the csv provider looks for bar files. |

## Backtesting

The one thing that needs no credentials, so it works today.

Drop bar files into `data/` named `TICKER_TIMEFRAME.csv`:

```
data/AAPL_D1.csv
openTime,open,high,low,close,volume
2026-01-02T00:00:00Z,100.00,101.00,99.00,100.00,1000
```

Set `MARKETDATA_PROVIDER=csv`, then:

```bash
curl -X POST http://127.0.0.1:8080/api/bot/backtest \
     -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"ticker":"AAPL","timeframe":"D1","thresholdPercent":4,"slippageBps":10}'
```

It writes nothing — the runner holds no repository, so a replay cannot touch the
account the live-trading gate reads.

**Read `docs/BACKTEST-SPEC.md` before trusting a number from it.** Three things
in particular:

- **Entry is at the next bar's open**, never the signal bar's close. Entering at
  the close is the classic lookahead bug and inflates any momentum rule.
- **A bar containing both the stop and the target is scored as a loss.** OHLC
  does not say which came first. The report counts these separately — if most of
  your results rest on that assumption, the timeframe is too coarse to conclude
  anything.
- **Below 30 closed trades the report withholds win rate and expectancy.** A win
  rate from eight trades is not an estimate of anything.

**What it does not test.** Only the percentage-move and gap maths. The
volume-to-open-interest and option-chart rules need option chains with open
interest, which equity bar files do not contain and which are not freely
available. Those thresholds remain untested guesses — a good equity backtest is
not evidence about them.

## Paper trading

**thinkorswim paperMoney has no API.** paperMoney lives inside the thinkorswim
platform; Schwab's Trader API places only real orders on funded accounts, and
its developer sandbox covers authentication and token refresh against synthetic
data — there is no simulated-fill mode and no paper endpoint. So paper trading
happens elsewhere.

Two options, chosen with `BOT_BROKER`:

| Value | What it does |
|---|---|
| `in-memory` (default) | Fills instantly at the intended price. No account needed. Right for testing plumbing, **useless for judging a strategy.** |
| `alpaca-paper` | Alpaca's paper host, with simulated fills against real market data. |

```bash
BOT_BROKER=alpaca-paper
APCA_API_KEY_ID=...          # PAPER keys only
APCA_API_SECRET_KEY=...
MARKETDATA_PROVIDER=alpaca
BOT_UNIVERSE=AAPL,MSFT,NVDA
BOT_ENABLED=true
```

**Paper keys only.** Alpaca issues separate key pairs for paper and live on
separate hosts, so a paper key is refused by the live host. That separation is
one of the two independent things keeping this bot away from real money — the
other being `LiveTradingGate`. There is deliberately no live URL anywhere in
this codebase.

### Two limits of Alpaca's free tier

- **The feed is IEX, a small share of consolidated volume.** Prices are fine, so
  the percentage-move rule is fine. **Volume is not.** Any volume threshold
  calibrated on IEX is calibrated against a few percent of what actually traded,
  so the strategy's "volume confirms" condition and `VolumeOpenInterestRule`
  cannot be validated on this feed. SIP is a paid subscription.
- **Historical bars lag about 15 minutes.** Harmless for daily bars; it means
  the bot runs on `D1` only, and the intraday timeframes stay backtest-only
  until there is a SIP subscription.

And as with any broker simulator, **Alpaca paper fills don't model market impact
or realistic slippage.** The reconciler records intended price against actual
fill on every order, which gives you measured slippage from your own order flow
— the number that should eventually replace the guessed `slippageBps` default in
the backtest's `CostModel`.

## Trade journal — learning what you don't put into words

Records each manual fill with a snapshot of the market at that instant, plus the
marked-up chart you were looking at. Then it replays the bot's own rules over
each snapshot and buckets the result:

| Bucket | Meaning |
|---|---|
| `AGREED` | You took it and a rule fired. Confirms what's already encoded. |
| `YOU_TOOK_RULES_SILENT` | **The articulation gap.** You traded it; no rule describes why. |
| `RULES_FIRED_YOU_PASSED` | An unstated veto — something disqualified it. |

The silent bucket is the output that matters: each entry is a decision made for
a reason the system doesn't contain, and the chart attached to it is the
shortest route to finding out what. `unexplainedWinners()` narrows it further —
a profitable trade no rule describes is the strongest evidence something real is
missing.

The rule book is the mirror image. State a rule and it measures your fills
against it:

> "I never buy below the previous close" was contradicted by 2 of 3 fills (66.7%).
> Either it's being broken, or it's not the rule actually being traded.

**Three things that bound what this can tell you:**

- **It only contains trades you took**, so it can't show what you correctly
  avoided. The passed-over bucket is a partial window, covering only setups the
  rules already flag.
- **Outcome is stored separately from your own judgement**, deliberately. A
  winning trade can be a bad decision; anything fitted to P&L alone learns to
  chase luck.
- **Rules naming an indicator can't be checked yet.** There's no indicator
  library, so `close > ema21` is refused at parse time rather than silently
  never evaluated. The checkable vocabulary is close/open/high/low/volume,
  previousClose, changePercent, rangePercent, and the option chain fields.

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
- **No backtest results exist yet.** The harness exists; the thresholds stay
  untested guesses until it is run over real history.
