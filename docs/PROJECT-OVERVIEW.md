# TCKR — Project Overview

A momentum swing-trading application: Spring Boot 2.6.6, Java 17, MySQL 8.0.
Product name is TCKR; the Java package root and Maven artifact are still
`com.moneyteam`, and both refer to the same thing.

- **Entry point:** `com.moneyteam.Television`
- **Scale:** 177 main sources, 33 test sources, 6 Flyway migrations
- **Tests:** 345 — 344 pass, 1 skipped (Docker-gated)
- **Governing docs:** `CLAUDE.md` (rules that outlive any phase), `PHASES.md`
  (what to build next)
- **Companion:** `docs/WORK-LOG.md` — every story and task completed, in order,
  with how it was done

> **Read "Honest state" at the bottom before drawing conclusions.** Several
> large pieces are deliberately unbuilt, and the system is explicit about which.

---

## 1. Intent

Scan a stock universe, produce a focus list, size positions from risk, and
manage exits mechanically. The strategy is momentum swing trading: strongest
stocks, in the strongest themes, during the strongest market environments.

The order of operations is fixed and every piece of code respects it:

1. **Market stage first** — Basing / Advancing / Topping / Declining, each with
   an exposure ceiling. In a declining market the correct number of new
   positions is zero.
2. **Top down** — market → theme → sector → leading stock → setup.
3. **Weekly for context, daily for the setup.**
4. **Tightness is the entry condition** — flags, wedges, inside days, bases,
   MA pullbacks, breakout retests: all volatility contracting before it expands.
   If price is not tight, the trigger and stop cannot be defined, so there is no
   trade.
5. **Volume confirms.**
6. **The plan precedes the position** — trigger, stop, size and first target are
   all computed before the order is placed.
7. **Scale out, never dump.**

---

## 2. Architecture

Organised **package-by-feature**, not by layer. Each feature package owns its
own controller / service / repository / model / dto.

```
com.moneyteam
├── user/        User, auth endpoints, registration
├── trading/     Trade, Order, OrderExecution, Position, Account, LedgerEntry
├── marketdata/  Bars, quotes, and four provider adapters
├── analysis/    indicators/, TechnicalAnalysis, FundamentalAnalysis, KeyMetrics
├── strategy/    Scan engine, rules, FVG detector, regime (planned)
├── alert/       Alerts with deduplication
├── bot/         Owner identity, live-trading gate, broker port, backtest, journal
├── watchlist/   Watchlist
├── common/      config, exception, security — shared, depends on no feature
├── demo/        PipelineDemo (not a Spring bean)
└── view/        UserInterface (legacy console scaffolding)
```

A feature package may depend on `common`. Cross-feature dependencies are rare
and deliberate; where one exists it is commented as such.

---

## 3. Security model

### Authentication

Stateless JWT. `/api/auth/login` and `/api/auth/refresh` return both tokens in
the response body, expected back in the `Authorization` header — no cookies, so
CSRF is disabled with reasoning recorded in `SecurityConfig`.

- Access and refresh tokens are distinguished by a `type` claim, so an access
  token cannot be replayed at the refresh endpoint.
- HS256 with a key of at least 32 bytes. **Startup fails on a shorter key**
  rather than issuing forgeable tokens.
- Login lockout and per-IP rate limiting (Bucket4j).
- Unknown user and wrong password fail identically, so usernames cannot be
  enumerated.

### Refresh tokens are single-use

`common/security/RefreshTokenStore` holds one row per issued token, keyed on its
`jti`. Redemption spends it through a conditional `UPDATE ... WHERE used_at IS
NULL`, so InnoDB's row lock decides the winner when two requests present the
same token.

A **second** redemption means two parties hold the token and the server cannot
tell owner from thief, so the user's **entire** token family is revoked. Per
RFC 9700. Never make redemption idempotent to smooth over a double-submitting
client: that removes the only signal theft produces.

### Authorization

Identity comes from the authenticated principal via
`common/security/CurrentUserService` — **never** from a `userId` in a request
body or path. Request DTOs deliberately have no user id; services take it as an
explicit parameter so the compiler catches a route that forgets. Reads of
another user's record return **not-found rather than forbidden**, so ids cannot
be probed.

### The live-trading gate

`bot/LiveTradingGate` holds two separate facts:

| Fact | Set by |
|---|---|
| **armed** | paper equity reaching a target fixed on first boot and immutable after |
| **unlocked** | an explicit owner action, never automatically |

They are never collapsed. If reaching the target unlocked by itself, an
arithmetic error in the equity calculation would be an error that trades real
money.

- **A SELL is never gated**, in either mode, even at ceiling 0.00. Blocking a
  close traps an open position, which is worse than no gate.
- Equity counts open positions at **cost**, not market value, so an unrealized
  spike cannot arm the gate — and a fully invested account can still arm, which
  cash-only accounting would have prevented.
- Unlocking requires echoing the exact target back. Not authentication — the
  caller is already authenticated — but it makes an unlock impossible by
  accident.

### Secrets

All from the environment, no defaults. A default credential is worse than a
missing one. SQL bind-parameter logging is confined to the `dev` profile, since
bind values contain passwords and PII.

### Errors

Mapped centrally in `common/exception/GlobalExceptionHandler`. Controllers do
not catch and echo `e.getMessage()`. Unanticipated failures return a correlation
id; the cause is logged server-side.

---

## 4. Data layer

**MySQL 8.0, InnoDB.** `mysql-connector-java` at runtime, `MySQL8Dialect`,
Hikari pool (max 10).

**Flyway owns the schema.** `ddl-auto=validate` in *every* profile, not just
production, so entity/schema drift fails at startup rather than surfacing as a
runtime query error.

| Migration | Contents |
|---|---|
| `V1__baseline_schema.sql` | users, accounts, positions, trades, orders, ledger, stocks |
| `V2__strategy_alerts_and_plans.sql` | alerts, trade plans |
| `V3__oauth_token_store.sql` | encrypted provider refresh tokens, version column for single-flight |
| `V4__refresh_token_rotation.sql` | one row per issued refresh token |
| `V5__bot_config.sql` | single-row bot config and the gate |
| `V6__trade_journal.sql` | journal entries, annotations, stated rules |

Tests use **Testcontainers MySQL 8.0, never H2** — H2 does not reproduce
MySQL's `ENUM` columns, FK semantics or dialect behaviour, so it can pass while
production fails. There is no H2 dependency at all.

---

## 5. Market data

One port, `marketdata/service/MarketDataProvider`, four adapters selected by
`app.marketdata.provider`:

| Value | Adapter | Notes |
|---|---|---|
| `in-memory` | `InMemoryMarketDataProvider` | Default. Fixtures; no credentials. |
| `csv` | `CsvMarketDataProvider` | `TICKER_TIMEFRAME.csv` on disk. Needs nothing. |
| `alpaca` | `AlpacaMarketDataProvider` | IEX feed, ~15-minute delay on the free tier. |
| `schwab` | `SchwabMarketDataProvider` | OAuth, rate limiting, retry, circuit breaking. |

Shared contract: **bars are oldest-first**, index `size()-1` is the most recent,
enforced by `Bar.requireOldestFirst` at every boundary. An out-of-order series
is **rejected, not silently sorted** — sorting hides a file assembled wrongly,
and every indicator depends on the ordering.

Prices are `BigDecimal` parsed from text, never via `double`. Gap detection is
an exact inequality between two prices, and binary floating point would make it
depend on representation error.

### Schwab specifics

- **Single-flight OAuth refresh** with a compare-and-set on a version column.
  Schwab refresh tokens are one-time use, so concurrent refreshes would race to
  spend the same value and strand the integration.
- A `MINIMUM_REFRESH_INTERVAL` guard: a token born expired would otherwise cause
  a refresh on every call, spending a one-time token each iteration.
- The refresh token is persisted **before** being published, so a crash between
  the two does not lose it.
- Encrypted at rest (AES-GCM).
- `BarAggregator` builds hourly bars from 30-minute pairs, because Schwab has no
  hourly frequency. Pairs run from the series start so hours are session-aligned,
  and a trailing partial bar is emitted rather than dropped.
- `OptionQuote` carries **two timestamps** — `asOf` and `openInterestAsOf` —
  because open interest publishes once daily and is a day stale relative to the
  quote.

---

## 6. Strategy engine

`strategy/engine/StrategyEngine` evaluates rules across a universe concurrently.

- A **bounded, owned thread pool**, not `parallelStream` — a parallel stream runs
  on the shared common ForkJoinPool, contending with everything else in the JVM.
- **One task per ticker**: tickers are independent, so no shared mutable state.
- **A semaphore around provider calls**: market data is the rate-limited
  bottleneck; without a gate, widening the pool converts throughput into 429s.
- **A per-task deadline**, so one unresponsive ticker cannot hold a scan open.
- **Deterministic output** — results are sorted, so a parallel scan and a serial
  one over the same fixture produce identical output.
- Failures are returned in `ScanResult.failures()`, never swallowed.

**Evaluation parallelises; acting does not.** An exposure ceiling is a global
constraint, and two threads checking remaining capacity at the same instant both
see room for one more position. Anything acting on signals is single-threaded.

### Rules and detectors

- `FairValueGapDetector` — 3-bar imbalance, **strict inequality** so a touching
  bar is not a gap. Also `detectUnfilled`.
- `PercentMoveRule` — configurable baseline (previous close / session open /
  N bars ago), so "up 4% from yesterday" and "up 2% in the last half hour" are
  one code path.
- `VolumeOpenInterestRule` — V/OI ratio with an absolute volume floor.
- `HighValueOptionRule` — V/OI plus gap confluence across timeframes.

Rules are pure functions of an `EvaluationContext` — one immutable snapshot per
ticker, so every rule reasons over the same instant and two rules cannot see
prices a minute apart.

### Indicators

`analysis/indicators`, specified in `docs/INDICATOR-SPEC.md` with hand-computed
vectors written **before** the implementations.

`IndicatorSeries` returns `Optional<BigDecimal>`; warm-up is an **absence**, not
a zero, because a zero would be indistinguishable from a genuine zero reading.

Settled decisions, each with its own test:

- **EMA is SMA-seeded.** Seeding from the first price converges to the same curve
  but differs for roughly `3n` bars — on a 50-period EMA, most of a trading year.
- **Wilder smoothing is `1/n`, not `2/(n+1)`.** Wilder(n) equals an EMA of period
  `2n−1`, so the wrong constant gives a materially faster indicator that still
  looks plausible and breaks no bound. One implementation, one test.
- **True range carries all three terms.** Using only `high − low` understates
  volatility exactly on the bars where it matters, and since size is
  `risk ÷ (trigger − stop)`, an understated range silently **oversizes**.

---

## 7. Backtest harness

`bot/backtest`, specified in `docs/BACKTEST-SPEC.md` — again, hand-computed
numbers written before the code.

Three decisions a backtest can quietly make in its own favour, settled
explicitly:

| Decision | Choice |
|---|---|
| When entry happens | bar `i+1`'s **open**, never bar `i`'s close |
| A bar holding both stop and target | scored a **loss** — OHLC cannot say which came first |
| Slippage | charged on **both** sides; a long buys higher *and* sells lower |

**No lookahead is structural**: at step `i` the rule receives
`bars.subList(0, i+1)`, so future bars are absent from the object, not merely
unused. `previousClose` comes from `bars[i-1]`, deliberately not from the
provider — for a CSV file the provider returns the last row, a future price at
every step but the last.

Outcomes are scored in **R** (multiples of amount risked). Below 30 closed
trades the report **withholds** win rate and expectancy, returning null rather
than the figure with a flag, so a caller must handle the absence.

The runner holds **no repository**, asserted structurally. A replay that booked
simulated fills into `Account` would feed the live-trading gate.

---

## 8. Broker layer

`bot/broker/BrokerPort` with two implementations:

- `InMemoryBroker` — the default and the test double. Fills immediately at the
  intended price: right for testing plumbing, useless for judging a strategy.
- `AlpacaBroker` — **paper host only**.

**thinkorswim paperMoney is not reachable.** It lives inside the thinkorswim
platform; Schwab's Trader API places only real orders, and its sandbox covers
authentication against synthetic data, not fills. Paper trading happens via
Alpaca or in-memory.

Alpaca issues **separate key pairs for paper and live on separate hosts**, so a
paper key is refused by the live host. Combined with no live URL existing
anywhere in the codebase, real money is out of reach for two independent
reasons.

Two decisions worth knowing:

- **Order placement is not retried; reads are.** Retrying an order whose response
  was lost is how one intended position becomes two. The client order id is the
  recovery path, and `InMemoryBroker` honours it as an idempotency key so that
  path is tested.
- **An unrecognised broker status maps to `OPEN`, not `REJECTED`.** A new state in
  a future API version must not read as a refusal, which would make the
  reconciler abandon a live order.

`FillReconciler` books **deltas, not totals**: an order polled at 30% then 80%
books 30 then 50, and a repeat poll books nothing. Our `Account` feeds the gate,
so a double-booked fill could arm it on money never made.

---

## 9. Trade journal

`bot/journal` — built to learn the rules that are traded but never written down,
for a trader who describes setups visually. **Nothing in it asks for a setup to
be described.**

It records the fill, a `ContextSnapshot` of the market at that instant, and the
marked-up chart screenshot. Then it replays the encoded rules over each snapshot:

| Bucket | Meaning |
|---|---|
| `AGREED` | Taken, and a rule fired. Confirms the known. |
| `YOU_TOOK_RULES_SILENT` | **The articulation gap.** Traded; no rule describes why. |
| `RULES_FIRED_YOU_PASSED` | An unstated veto. |
| `NOT_EVALUABLE` | Snapshot missing or too thin — reported, not dropped. |

`unexplainedWinners()` narrows further: a profitable trade no rule describes is
the strongest evidence something real is missing.

`RuleBook` is the mirror image — state a rule and it audits your fills:

> "I never buy below the previous close" was contradicted by 2 of 3 fills
> (66.7%). Either it is being broken, or it is not the rule actually being traded.

Design points:

- **A snapshot must replay to the *same signal*** the live rules produced,
  asserted on the signal's own evidence map. A lossy reconstruction would make
  every disagreement an artefact that reads like a finding.
- **Fidelity is a column** — `UNDERLYING_ONLY`, `UNDERLYING_PLUS_EOD_CHAIN`,
  `FULL_CHAIN_AT_FILL`. Analyses are scoped to one tier, and `RuleBook` strips
  chain features from entries whose tier lacks them.
- **Judgement is separate from R**, and `judge()` cannot see an outcome. A winner
  the trader would not repeat is what a P&L-fitted model learns wrongly from.
- **Absence is never compliance** — a rule whose feature is missing reports
  *uncheckable*, never upheld.
- **Uploads are untrusted** — type from magic bytes not the declared type or
  extension, generated filenames, size cap, root containment checked after
  normalisation.

---

## 10. Money and precision

Anything representing cash, equity, realised P&L or an order value is
`BigDecimal` with an explicit `RoundingMode`. Raw market-data prices may be
`double`.

**Where the boundary sits:** a value becomes money the moment it is multiplied by
a quantity, settled against an account, or persisted to a position, ledger or
trade. A quote you fetched and are about to display is market data. The same
number, multiplied by share count to debit buying power, is money.

Intermediate arithmetic is carried at full precision; only emitted values are
rounded, to scale 4 `HALF_UP`.

---

## 11. Testing approach

**The test is written alongside the code, never afterwards.** For any indicator
or strategy calculation this is stronger: hand-computed expected values are
written *before* the implementation, which forces the ambiguous decisions — EMA
seeding, Wilder vs standard smoothing, bar alignment, week boundaries — to be
settled as specification rather than frozen by whatever the first implementation
happened to do.

That approach has caught real bugs; see `docs/WORK-LOG.md`.

- Testcontainers MySQL, never H2
- WireMock for provider contract tests, so retry/rate-limiting/circuit-breaking
  are exercised without a key
- Structural assertions where behaviour alone is insufficient — e.g. the
  backtest runner is asserted to hold no repository, not merely to perform no
  writes during one run

---

## 12. Running it

```bash
./scripts/demo.sh                  # analytical core, no DB/credentials/Docker
```

```bash
cp .env.example .env               # DB_USER, DB_PASS, MYSQL_ROOT_PASSWORD, JWT_SECRET
openssl rand -base64 48            # JWT_SECRET
docker compose up --build
```

`docs/DEMO.md` has the full walkthrough with curl commands.

**`mvn test` reporting `345 passed, 0 skipped` means the schema is verified.**
`344 passed, 1 skipped` means it is not — `SchemaMigrationTest` is Docker-gated
and is the only test that applies the migrations to real MySQL.

---

## 13. Honest state

### Built and working

Package-by-feature layout · JWT auth with single-use refresh tokens ·
principal-based authorization · order/execution state machine with partial fills
· PnL with an oversell guard · account and ledger with buying-power checks ·
Flyway schema ownership · four market-data adapters · concurrent strategy engine
· FVG detector · percentage-move and volume/OI rules · alerts with deduplication
· staged trade plans that never place an order · backtest harness · broker port
with paper adapter and idempotent fill reconciliation · trade journal with
disagreement mining · indicator library · Docker stack · runnable demo

### Not built — do not assume these exist

- **No market-stage gate.** The exposure ceiling is documented in `CLAUDE.md` and
  **not enforced anywhere**. This is the first strategy invariant and the most
  significant gap. The live-trading gate is a different thing and no substitute:
  it decides whether real money is reachable at all, not how much exposure the
  market stage permits.
- **No live broker adapter and no live broker configuration.**
- **No backtest results.** The harness exists; nothing has been run over real
  history, so **the rule thresholds are untested guesses.**
- **No signal comes from real market data.** The Schwab adapter has never run
  against the live API.
- **No learning layer.** The journal collects evidence; nothing fits anything to
  it. When it arrives it tunes **ranking weights only** — never size, stop or the
  exposure ceiling — withheld below a sample floor.
- **No regime classification, setup detection, scale-out ladder** or adaptive
  weighting.
- **No front end.**

### Known defects, recorded not hidden

- `analysis/model/RiskManagement` holds static mutable state — per-instance data
  in `static` fields silently returns another ticker's numbers.
- `trading/model/Options.expiration` is a `String`, not a `LocalDate`. The
  journal uses `LocalDate` rather than copying the mistake.
- `marketdata/model/Stock` still holds `Double` for `ask`, `bid`, `open`,
  `close`, `last`, `marketCapAmount`.
- `PnLCalculator.realizedPnL` pins scale 4 but `unrealizedPnL` does not.
- EXIF is not stripped from uploaded screenshots.
- Five dead files await deletion (`StockTradingController`, `StockTradingService`
  + impl, `PurchaseTrade`, `StockTradeRequest`, `OptionsTradeRequest`).
- **Volume-based rules are unvalidated** on Alpaca's free IEX feed, which is a
  small share of consolidated volume.

### Outstanding security action

Schwab API credentials — a `client_id`, a `client_secret`, an OAuth access token
and two authorization codes — were committed to this repository, which was
**public from 2025-08-03 until the GitGuardian alert**, roughly fourteen months.
They **must be treated as compromised.**

History has been rewritten and the values are redacted from all nine branches.
Two things that rewrite did **not** achieve:

- **GitHub's `refs/pull/*` refs still carry the original commits** (`f2c0c64`,
  `1bca748`). They are server-managed and cannot be force-pushed or deleted.
  Only a GitHub Support request to garbage-collect unreachable objects removes
  them; making the repository private denies anonymous access meanwhile.
- **Rotation at the Schwab developer portal is still outstanding.** It is the
  only action that makes already-published values worthless.
