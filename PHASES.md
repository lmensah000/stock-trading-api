# TCKR — implementation phases

Read `CLAUDE.md` first. Do one phase at a time. Compile after each step, test
before marking a phase done, show the plan before editing.

This file supersedes the earlier standalone phase plans. There is **one**
numbering scheme; the old "Phase 2 = auth" / "Phase 3 = model" numbering is
retired to avoid ambiguity about which plan a phase number refers to.

| Phase | Scope | Status |
|---|---|---|
| 0 | Security hardening | **DONE** ¹ |
| 1 | Boot blockers, bug fixes, package-by-feature reorg | **DONE** |
| 2 | Core domain model | **PARTIAL** |
| 3 | Schema ownership: migrations + money precision | **NEXT** |
| 4 | Test foundation + math reference vectors | **NEXT** |
| 5 | Market-data provider layer | |
| 6 | Indicator library (test-first) | |
| 7 | Strategy engine (test-first) | Future |
| 8 | Dashboard | Future |
| 9 | Alternative data | |
| 10 | Ops: CI, Docker, observability | Parallel |

¹ Code work complete. **Credential rotation at the Schwab developer portal is
still outstanding and is a human action.**

---

## Testing is a track, not a phase

There is deliberately **no trailing "write the tests" phase**. Tests are written
with the code, and for anything mathematical the expected values are written
*before* it. An earlier draft of this plan put all strategy testing at the end;
that is backwards for logic that decides what gets bought and when it is sold.

This is why Phase 4 sits early and why Phases 6 and 7 are marked test-first.

---

## Phase 0 — Security hardening ✅ DONE

Landed: secrets removed from the working tree and `.gitignore` added; stateless
JWT auth (`/api/auth/login`, `/api/auth/refresh`) replacing HTTP Basic; `Role`
enum; login lockout and per-IP rate limiting; identical failure for unknown user
and wrong password; SQL bind-parameter logging confined to `dev`; `dev`/`prod`
profiles; `User.toString()` no longer prints the password hash; centralised error
handling that does not echo exception text.

**Still outstanding:** rotate the leaked Schwab `client_id` / `client_secret` /
bearer token. They remain in git history. No history rewrite was performed, by
decision — rotation is what closes the exposure.

## Phase 1 — Boot blockers and bug fixes ✅ DONE

Landed: package-by-feature reorg; `@RestController` added where missing;
`TradeController` create route fixed; `TradeType` split into `OrderSide`;
`Stock.last` de-`static`ed; `Watchlist` table name fixed; `OptionsTradeRepository`
return type corrected to its root entity; `StockTradeRepository` id type corrected
to `String`; duplicate `status` branch removed from trade search;
`OptionsTradeRequest.setStrategy` added; `getTradeHistory` / `getUserPositions`
implemented; `createNewPosition` now sets the scalar FK.

## Phase 2 — Core domain model 🔄 PARTIAL

**Done:** `Order` / `OrderExecution` state machine with enforced legal
transitions and partial fills; `PnLCalculator` (pure, no Spring/DB) with realized
P&L and an oversell guard; `Account` / `LedgerEntry` with buying-power checks on
BUY and proceeds credited on SELL; principal-based authorization throughout.

**Remaining:**
- **Short selling** — extend `OrderSide` with `SELL_SHORT` / `BUY_TO_COVER`, add
  `positionSide` (LONG/SHORT) to `Position`, make `PnLCalculator` sign-aware, and
  convert `updatePosition` to an exhaustive switch with the mirror over-cover
  guard.
- **`TradingCalendar` / `MarketSession`** — NYSE/NASDAQ holidays and early
  closes; `PRE_MARKET` / `REGULAR` / `AFTER_HOURS` / `CLOSED` derived in
  `America/New_York`. Used to reject orders when the market is closed, and a
  prerequisite for gap-filling in Phase 5. Share fixtures with the weekly-rollup
  work in Phase 4 — week boundaries and holiday handling are the same problem.
- **`CorporateAction`** — `{ticker, type: SPLIT/DIVIDEND/MERGER/SPINOFF,
  effectiveDate, ratio, amount}`, replacing the bare dividend boolean and feeding
  adjusted historical prices.
- **`KeyMetrics`** — nine fields are declared but only P/E has a setter, so the
  class cannot be populated. Add the missing accessors.
- **`RiskManagement`** — currently a field-for-field copy of `Stock` with all
  state `static` (violating a `CLAUDE.md` invariant) and no risk logic. Replace
  with real stateless calculations: max position size as a percentage of account,
  position size from a stop distance, and sector-concentration limits.

  *Note:* an earlier plan called for deleting `RiskManagement` and
  `TechnicalAnalysis` outright because `ScaleOutManager` and `Indicators` replace
  them. Those classes do not exist yet, so deleting now leaves a hole. Repurpose
  them; revisit deletion when Phases 6–7 land.

## Phase 3 — Schema ownership and money precision 🎯 NEXT

Two migrations that belong in one wave, because both rewrite the schema.

- **Flyway.** Convert the hand-written `src/main/resources/sql/tableSchema.sql`
  into versioned migrations; set `ddl-auto=validate` once Flyway owns the schema.
  Include a reset flow for any pre-existing rows whose passwords predate hashing.
- **`double` → `BigDecimal`.** `marketdata/model/Stock` still holds `ask`, `bid`,
  `open`, `close`, `last` and `marketCapAmount` as `Double`. Apply the boundary
  rule in `CLAUDE.md`: values that get multiplied by quantity, settled, or
  persisted to an account/ledger/position are money. List every field before
  changing one.

**Why now:** three merged commits have already changed the schema with no
migration behind them. Migrations must own the schema before more model changes
land. The money conversion touches the same tables, so doing both at once is one
migration wave instead of two.

**Exit:** `mvn test` passes with `ddl-auto=validate` against a Flyway-built
schema; no money field is a floating-point type outside raw quote ingestion.

## Phase 4 — Test foundation and math reference vectors 🎯 NEXT

There is no `src/test` directory today. This phase has two halves, and the second
is the point.

**1. Harness and existing-logic tests.** JUnit 5, AssertJ, Mockito; Testcontainers
with MySQL (matching the real dialect rather than H2); WireMock for external HTTP.
Then cover the logic that exists and is currently untested:
- `PnLCalculator` — long and short, partial exits, rounding.
- Order state machine — every legal transition and every rejected one.
- `TradeServiceImpl.updatePosition` — BUY averaging, SELL realized P&L, the
  oversell guard.
- Buying-power rejection, and the authorization boundary: user A cannot read or
  act on user B's orders, trades, or account.

**2. Hand-computed reference vectors, written ahead of the implementations.**
Fixture series plus expected values. These need no production code to exist and
become the executable specification that Phases 6 and 7 are built against,
red-to-green. Priority order is exactly where these go subtly wrong while still
looking plausible:

| Priority | What | The trap |
|---|---|---|
| 1 | EMA seeding | SMA-seeded and first-value-seeded diverge for many bars |
| 2 | Wilder smoothing (ATR, RSI) | Wilder's `1/n` is not the standard `2/(n+1)` |
| 3 | Weekly rollup from daily bars | Week boundaries, partial trailing weeks, holiday-shortened weeks |
| 4 | Contraction ratio / tightness | It is the entry condition, so an error changes what gets bought |
| 5 | SMA, MACD, Bollinger, VWAP | VWAP needs volume, so it operates on bars not closes |

Derive vectors from published reference series and recompute them independently,
so a failure means the implementation is wrong rather than that it agrees with
itself.

**Exit:** `mvn test` green; every vector table committed and referenced by a test
that currently fails for want of an implementation, or passes against existing
code.

## Phase 5 — Market-data provider layer

Replace the scattered single-purpose clients (a malformed unauthenticated Yahoo
call, a dummy endpoint, a local Flask stub, an OAuth client with no refresh
handling).

- `MarketDataProvider` interface: `getQuote`, `getHistoricalBars`,
  `getCorporateActions`. Bars **oldest-first**.
- **Propose two or three providers with rate limits and pricing before choosing.**
  This decision constrains whether later phases are even feasible — history
  depth, request ceilings and cost all bound a universe scan.
- Resilience4j circuit breaker, retry with jittered backoff, and rate limiter on
  every outbound call.
- Caching keyed by ticker + date, with a short TTL for quotes and a long one for
  historical bars. **Show the cache layer before wiring the provider** — getting
  this wrong means a bill or a ban on the first real universe scan.
- Replace the hand-rolled OAuth token handling with Spring Security's
  `OAuth2AuthorizedClientManager`.

**Gap-filling — the technique must match the data type:**
- **Trade prints:** never interpolate. A missing print is a real absence of
  activity. Mark `DATA_GAP`.
- **OHLCV bars for indicators:** forward-fill (LOCF, zero volume) for feed
  hiccups during open periods. **Never linear-interpolate a price series** — it
  fabricates a plausible path that generates false signals.
- **Halts:** distinguish from feed gaps via `TradingCalendar` plus provider halt
  flags; mark `HALTED` and exclude from indicator windows.
- **Holidays/weekends:** not gaps at all. `TradingCalendar` skips them when
  building windows.

**Exit:** a WireMock contract test proves retry and circuit-breaking actually
engage on simulated 429 and 5xx responses.

## Phase 6 — Indicator library (test-first)

`analysis/indicators` as pure functions with no Spring or DB dependency — the
highest test-value code in the repository. Implement against the Phase 4 vectors:
SMA, EMA, RSI (Wilder), MACD, Bollinger, VWAP, ATR, weekly rollup, contraction
ratio.

`StockStrategiesImpl.shouldBuy` / `shouldSell` currently compare price against
hardcoded thresholds (`> 50`, `< 40`). They compose real indicators once these
exist.

**Exit:** every indicator passes its hand-computed vectors; no bare price
thresholds remain in strategy code.

## Phase 7 — Strategy engine (future, test-first)

The momentum system described in `CLAUDE.md`. None of this exists yet; it is the
intended direction, gated on Phases 5 and 6.

- **Market regime** — Basing / Advancing / Topping / Declining with exposure
  ceilings 1.00 / 0.30 / 0.25 / 0.00, from index trend, breadth (share of the
  universe above its own 50-day average) and follow-through rate (share of the
  last 20 breakout entries that held above trigger — computed from our own trade
  history, not a vendor). *Test: strong index with weak breadth and weak
  follow-through must yield `TOPPING`.*
- **Setup detection** — classify flags, wedges, inside days, bases, MA pullbacks
  and breakout retests; assert classification **and stop placement** against
  fixture bar series per setup type.
- **Risk sizing** — `shares = (equity × riskPercent) ÷ (trigger − stop)`.
  Deterministic and auditable.
- **Order path** — one entry point that checks the exposure ceiling and **rejects
  with a reason** above it. A ceiling of zero rejects everything. Persist a
  `TradePlan` at order time: trigger, stop, shares, first target, and the full
  check list that justified it. That record is the only honest way to review a
  losing trade.
- **Scale-out ladder** — trim into the first target, stop to breakeven, trim at
  3×ATR above the 8 EMA, then trail 8/21/50 EMA. A scheduled end-of-day job that
  is **idempotent**: keyed by position + date + rung, so running it twice does
  not sell twice. *Tests: full four-rung ladder, early stop-out, gap through the
  stop, and an attempt to lower a stop (must be impossible).*
- **Adaptive weighting** — tunes ranking weights only, never size or stops. No
  movement below a minimum sample count; clamp to a fixed band. Then run it
  against real trade history and **report honestly whether it learns or fits
  noise.**
- **Nightly scan** — persists a focus list after the close. Places no orders.

## Phase 8 — Dashboard (future)

No front end exists in this repo. **Before building one, evaluate the React +
Tailwind frontend on the `main`/`sta` branches** — it already implements
portfolio, watchlist, quotes and trade history against the same product concept.
Adapting it is likely cheaper than building fresh. If a new dashboard is built
instead, it authenticates with the JWT bearer token and degrades to a visible
"data unavailable" state rather than a blank screen or fabricated sample data.

## Phase 9 — Alternative data

Scope honestly: this category is **not retail-obtainable in real time**. Each
source is a batch ingest with its own cadence, and every record carries both the
event date and the publication date.

- **FINRA ADF/ORF dark-pool volume** — T+1/T+2 lag, daily batch.
- **SEC Form 4 insider transactions** — filed within ~2 business days; poll every
  few hours, not live.
- **SEC 13F institutional holdings** — quarterly, up to 45 days stale. This is the
  one most often mistaken for current; label it explicitly.
- A shared freshness record (`REAL_TIME` / `NEXT_DAY` / `QUARTERLY`) so every API
  response carries an "as of" and a staleness class.

**Explicitly out of scope:** print-by-print real-time dark-pool detail and
non-public insider information. No legitimate retail feed provides these.

## Phase 10 — Ops (parallel)

CI running `mvn -B verify` plus static analysis on every PR; multi-stage
Dockerfile and a compose file (app + MySQL + Redis); Actuator and Micrometer;
Redis-backed caching and rate limiting to replace the current in-memory lockout
and throttle, which are per-instance and do not survive scaling out.

---

## Related branches — reference only

`master` is canonical. `main` and `sta` carry a separate implementation of the
same product on a lineage with **no shared git history**: a React + Tailwind
frontend, a FastAPI + MongoDB backend, a substantial passing test suite, a
product requirements document, and a copy of this app's pre-reorg Java code.

**Do not merge them.** Mine them: the requirements doc as a specification source,
the existing tests as behavioural cases worth porting, the frontend as a
candidate for Phase 8. Recorded here so the work is not rediscovered or
duplicated later.

The `em` branch is an unrelated fitness application.
