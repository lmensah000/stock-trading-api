# Demo

Two parts, because they need different things.

**Part 1 runs anywhere** — no database, no credentials, no Docker. It walks the
analytical core: bars, signals, gaps, sizing, backtest, broker, and the journal's
articulation-gap analysis.

**Part 2 needs MySQL** — the HTTP API, Flyway and the live-trading gate. This is
the real product, and as of writing **it has never actually been booted.**

---

## Part 1 — The pipeline walkthrough

```bash
./scripts/demo.sh
```

Seven sections, each calling the real production classes. A demo that
reimplemented any of it would demonstrate the demo rather than the system.

What to look at:

| Section | The point |
|---|---|
| 1. Bars | Read back through the real CSV parser, so `requireOldestFirst` is in the path. |
| 2. Signals | `StrategyEngine` output with the full evidence map, so a signal can be audited. |
| 3. Gaps | A *touching* bar is not a gap — strict inequality, no floating-point dependence. |
| 4. Sizing | A wider stop buys fewer shares and **both risk the same 1,000**. The invariant, visible. |
| 5. Backtest | Zero-cost vs 10bps side by side, so the cost effect is a number rather than a claim. |
| 6. Broker | Resubmitting a client order id returns **one** order, not two. |
| 7. Articulation gap | Trades taken while every rule stayed silent, and a stated rule the fills contradict. |

### Read section 5 carefully

**The bar series is generated.** The R figures show the harness works. They are
**not** a backtest result and not evidence about the strategy. The runner prints
that caveat in its own output, twice, so a screenshot of it cannot be mistaken
for a finding.

Nothing has been backtested over real history, so the rule thresholds remain
untested guesses.

### Section 7 is the interesting one

```
4 entries at FULL_CHAIN_AT_FILL against 1 rule(s): 1 agreed,
3 taken with every rule silent. The 3 silent one(s) are the rules
you have not written down yet.

AGREED                   DEMO  R=-      rules=[PercentMoveRule(4% from PREVIOUS_CLOSE)]
YOU_TOOK_RULES_SILENT    DEMO  R=2.4    rules=(silent)
YOU_TOOK_RULES_SILENT    DEMO  R=1.1    rules=(silent)
YOU_TOOK_RULES_SILENT    DEMO  R=-1.0   rules=(silent)

unexplained trades: 3, of which winners: 2
```

Then the mirror image:

```
"I only buy above the previous close" held on all 4 checkable fill(s).
"I need at least a 4% move" was contradicted by 3 of 4 fill(s) (75.0%).
    Either it is being broken, or it is not the rule actually being traded.
```

This needs no database and works on your own fills the moment you start
recording them.

### Not in Part 1, deliberately

`LiveTradingGate` reads three repositories. Faking those so the demo could print
a reassuring line about real money being locked would be exactly the wrong thing
to fake. `LiveTradingGateTest` covers it.

---

## Part 2 — The full stack

### Start it

```bash
cp .env.example .env
openssl rand -base64 48        # JWT_SECRET; under 32 bytes and startup refuses
# fill in DB_USER, DB_PASS, MYSQL_ROOT_PASSWORD, JWT_SECRET
docker compose up --build
```

Flyway applies V1–V6 and `ddl-auto=validate` then reconciles every entity
against the result. **A boot that succeeds is itself the proof** that the schema
and the entities agree. A boot that fails on validation means they have drifted —
that is the check working.

### 1. Verify the schema — the most valuable thing this unlocks

```bash
mvn test
```

**332 passed, 0 skipped** means the schema is verified. `331 passed, 1 skipped`
means it is not: `SchemaMigrationTest` is Docker-gated and is the only test that
applies the migrations to real MySQL 8.0. It has never run.

### 2. Register and log in

```bash
curl -X POST http://127.0.0.1:8080/api/users/register \
  -H 'Content-Type: application/json' \
  -d '{"userName":"you","passWord":"a-password-you-choose","email":"you@example.com"}'

TOKENS=$(curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"userName":"you","passWord":"a-password-you-choose"}')
echo "$TOKENS"

TOKEN=$(echo "$TOKENS"  | python3 -c 'import json,sys;print(json.load(sys.stdin)["accessToken"])')
REFRESH=$(echo "$TOKENS" | python3 -c 'import json,sys;print(json.load(sys.stdin)["refreshToken"])')
```

### 3. Watch single-use refresh tokens work

Worth doing, because it is the one security property easiest to believe is
working when it is not. Redeem the **same** refresh token twice:

```bash
curl -s -X POST http://127.0.0.1:8080/api/auth/refresh \
  -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$REFRESH\"}"
# -> a new token pair

curl -s -X POST http://127.0.0.1:8080/api/auth/refresh \
  -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$REFRESH\"}"
# -> 401. The replay revoked the whole family, so the pair issued above is
#    dead too. That is intended: the server cannot tell owner from thief.
```

### 4. Check the gate is shut

```bash
curl -s http://127.0.0.1:8080/api/bot/status -H "Authorization: Bearer $TOKEN"
# -> {"mode":"UNCONFIGURED","armed":false,"live":false}   (bot disabled)

curl -s -X POST http://127.0.0.1:8080/api/bot/unlock \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"confirmTargetEquity":130000}'
# -> 423 Locked. Refused because it is not armed.
```

### 5. Run a backtest over the API

```bash
# data/AAPL_D1.csv  ->  openTime,open,high,low,close,volume
# then restart with MARKETDATA_PROVIDER=csv

curl -s -X POST http://127.0.0.1:8080/api/bot/backtest \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"ticker":"AAPL","timeframe":"D1","thresholdPercent":4,"slippageBps":10}'
```

Expect `"lowSample": true` and `"winRate": null` until you have 30+ closed
trades. That is the harness refusing to quote a statistic it cannot support.

### 6. Only then, the bot

```bash
BOT_ENABLED=true
BOT_OWNER_USERNAME=you          # must already be registered
BOT_UNLOCK_TARGET_EQUITY=130000 # immutable after first boot — choose deliberately
BOT_BROKER=alpaca-paper         # PAPER keys only
BOT_UNIVERSE=AAPL,MSFT,NVDA
```

Health is `GET /actuator/health`, the only actuator path exposed.

---

## What is still not true after all of this

- **No signal comes from real market data** until the Schwab credentials are
  rotated or Alpaca keys are configured.
- **No live broker adapter exists.** There is no live URL in the codebase and
  the Alpaca keys are paper keys, which the live host refuses.
- **The market-stage exposure ceiling is not enforced.** `CLAUDE.md` specifies
  it as a hard cap in the order path; it is documented, not applied. Build it
  before unlocking anything.
