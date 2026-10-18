# CLAUDE.md — TCKR trading application

Read this before making any change. It describes what the app is, the rules the
domain imposes, and the things that are not open for redesign.

For *what to build next*, see `PHASES.md`. This file holds the rules that outlive
any particular phase.

## What this is

TCKR, a Spring Boot swing-trading application. Product name is TCKR; the Java
package root and Maven artifact are still `com.moneyteam`, and both names refer
to the same thing.

The intent is to scan a stock universe, produce a nightly focus list, size
positions from risk, and manage exits mechanically. **Today the repository is an
API only** — the scanning, sizing and exit engine described below is the
direction, not the current state. See "Current state" at the bottom for the
honest inventory.

- Spring Boot 2.6.6, Java 17, Maven, MySQL, `javax.*` (not `jakarta.*`).
- Entry point: `com.moneyteam.Television`.

## Layout

Organised **package-by-feature**, not by layer. Each feature package owns its own
`controller` / `service` / `repository` / `model` / `dto`:

```
com.moneyteam
├── user/        User, auth endpoints, registration
├── trading/     Trade, Order, OrderExecution, Position, Account, LedgerEntry
├── marketdata/  Stock, Crypto, external market-data clients
├── analysis/    TechnicalAnalysis, FundamentalAnalysis, KeyMetrics, strategies
├── watchlist/   Watchlist
├── common/      config, exception, security — shared, depends on no feature
└── view/        UserInterface (legacy console scaffolding)
```

A feature package may depend on `common`. Cross-feature dependencies should be
rare and deliberate. When the strategy engine arrives it goes in `strategy/` and
depends on nothing above it.

## The strategy this app implements

Momentum swing trading. Strongest stocks, in the strongest themes, during the
strongest market environments. The order of operations is fixed and every piece
of code should respect it:

1. **Market stage first.** Basing / Advancing / Topping / Declining. Each stage
   carries an exposure ceiling (1.00 / 0.30 / 0.25 / 0.00). In a declining market
   the correct number of new positions is zero. This is a hard cap enforced in
   the order path, not a warning on a screen.
2. **Top down.** Market → theme → sector → leading stock → setup. A perfect setup
   on a stock with no relative strength is not a trade.
3. **Weekly for context, daily for the setup.** Near highs, limited overhead
   supply, real base, trend intact — then look at the daily.
4. **Tightness is the entry condition.** Flags, wedges, inside days, bases,
   moving-average pullbacks, breakout retests. All the same thing: volatility
   contracting before it expands. If price is not tight, the trigger and the stop
   cannot be defined, so there is no trade.
5. **Volume confirms.** Expansion on the advance, contraction in the base, volume
   returning on the breakout.
6. **The plan precedes the position.** Trigger, stop, size and first target are
   all computed before the order is placed.
7. **Scale out, never dump.** Trim 25% into the first target → stop to breakeven →
   trim again at 3x ATR above the 8 EMA → trail the 8, 21 and 50 EMA, selling a
   piece on each daily close below.

## Invariants — do not violate these

Each is tagged with whether it is enforced in code today or describes a rule that
must hold once the relevant code exists. Do not mistake a `[future]` rule for a
guarantee the system currently provides.

- **`[enforced]` A user may only act on their own account.** Take identity from
  the authenticated principal via `common/security/CurrentUserService`. Never
  from a `userId` field in a request body or a path variable. Request DTOs
  deliberately have no user id; services take it as an explicit parameter so the
  compiler catches a route that forgets. Reads of another user's record return
  not-found rather than forbidden, so ids cannot be probed.
- **`[enforced]` Secrets come from the environment.** No credential is committed.
  SQL bind-parameter logging is never enabled outside the `dev` profile — bind
  values contain passwords and PII.
- **`[enforced]` A refresh token may be redeemed once.** Redemption spends the
  token and issues a replacement; `common/security/RefreshTokenStore` holds one
  row per issued token, keyed on its `jti`. A second redemption means two
  parties hold the token and the server cannot tell owner from thief, so the
  user's whole set is revoked rather than that one call refused. Never make
  redemption idempotent to smooth over a double-submitting client — that
  removes the only signal theft produces. Access tokens deliberately carry no
  `jti` and are not checked against the store.
- **`[enforced]` Real money is unreachable until the gate is opened by hand.**
  `bot/LiveTradingGate` holds two separate facts: *armed*, set when paper equity
  reaches a target fixed on first boot and immutable thereafter, and *unlocked*,
  set only by an explicit owner action. Never collapse them — if reaching the
  target unlocked by itself, an arithmetic error in the equity calculation would
  be an error that trades real money. **A SELL is never gated**, in `PAPER` or
  `LIVE`: blocking a close traps an open position, which is worse than no gate.
  Equity counts open positions at *cost*, not market value, so an unrealized
  spike cannot arm the gate. This gate is not the market-stage ceiling below,
  and must not be mistaken for it.
- **`[enforced]` The bot's identity comes from configuration, not a request.**
  `bot/BotIdentity` resolves the single owner from `app.bot.owner-username`.
  A scheduled scan has no authenticated principal, so this is the bot's
  equivalent of `CurrentUserService` — and like it, the client cannot influence
  it. Services still take the user id as an explicit parameter.
- **`[enforced]` Errors do not leak internals.** Exceptions are mapped centrally
  in `common/exception/GlobalExceptionHandler`. Controllers do not catch and echo
  `e.getMessage()`. Unanticipated failures return a correlation id; the cause is
  logged server-side.
- **`[partial]` No static mutable state in the model or strategy packages.**
  Per-instance data in `static` fields silently returns another ticker's numbers.
  `Stock.last` was fixed. **`analysis/model/RiskManagement` still violates this**
  and is scheduled for replacement in Phase 2.
- **`[future]` Position size comes from risk, never conviction.**
  `shares = (equity × riskPercent) ÷ (trigger − stop)`. A wider stop buys fewer
  shares automatically.
- **`[future]` Stops only ever move up.** No code path may lower a stop. Make this
  structurally impossible rather than conventional — a value object or a setter
  that rejects a lower value. This is far cheaper to establish before `Position`
  grows more setters than to retrofit afterwards.
- **`[future]` No path sells an entire position in one action**, except a stop-out
  or the final rung of the ladder.
- **`[future]` The learning layer tunes ranking weights only.** It must never
  influence position size, stop placement, or the exposure ceiling. Those stay
  deterministic and auditable.
- **`[future]` Price bars are always ordered oldest-first.** Index `size()-1` is
  the most recent bar. Every indicator will assume this.
- **`[future]` Trade outcomes are measured in R** (multiples of amount risked),
  not dollars.

## Money and precision

Anything representing cash, account equity, realised P&L, or an order value must
be `BigDecimal` with an explicit `RoundingMode`. Raw market-data prices may be
`double`.

**Where the boundary sits:** a value becomes money the moment it is multiplied by
a quantity, settled against an account, or persisted to a position, ledger or
trade. A quote you fetched and are about to display is market data. The same
number, multiplied by share count to debit buying power, is money.

Current state: `Trade`, `Position`, `Account` and `LedgerEntry` use `BigDecimal`.
`marketdata/model/Stock` still holds `Double` for `ask`, `bid`, `open`, `close`,
`last` and `marketCapAmount`. That conversion is Phase 3 and touches the schema —
if you find another `double` holding money, flag it before changing it.

## How I want you to work

- Work one phase at a time. Show the plan, wait for approval, then implement.
- Run `mvn compile` after every phase and `mvn test` before saying a phase is
  done. A phase that does not compile is not finished.
- Never delete a file without saying what was in it and why it goes.
- When you find a bug that was not mentioned, say so rather than fixing it
  quietly inside an unrelated change.
- Prefer constructor injection over `@Autowired` fields.
- **Write the test alongside the code, never afterwards.** For any indicator or
  strategy calculation this is stronger: the hand-computed expected values are
  written *before* the implementation. That forces the ambiguous decisions — EMA
  seeding, Wilder vs standard smoothing, bar alignment, week boundaries — to be
  settled as specification rather than frozen by whatever the first
  implementation happened to do.
- If a requirement here conflicts with the codebase, the codebase is probably
  wrong, but ask before assuming.

### If the work drifts

- "You changed more than the phase asked for. Show a diff of only what the phase
  required and revert the rest."
- "You let the learning layer influence sizing. Re-read the invariants."
- "That test passes because it asserts the behaviour you wrote rather than the
  behaviour specified. Rewrite it from this file."
- "Don't add a new abstraction for this. Show the version with the least new
  code."

## Current state

**Done:**
- Package-by-feature layout; the boot blockers and dead-code defects are fixed.
- Stateless JWT auth (`/api/auth/login`, `/api/auth/refresh`) replacing HTTP
  Basic, with a `Role` enum, login lockout and per-IP rate limiting. Refresh
  tokens are single-use, with replay detection that revokes the user's whole
  set.
- Principal-based authorization across every trading route.
- Order/`OrderExecution` state machine with enforced legal transitions and
  partial fills; `PnLCalculator` with realized P&L booking and an oversell guard;
  `Account`/`LedgerEntry` with buying-power checks on BUY and proceeds on SELL.
- Log and error-response hygiene; `dev` and `prod` profiles.
- A `bot` package with a single configured owner identity, a seeded paper
  account, and the live-trading gate described in the invariants.
- A Docker image and a `docker compose` stack with MySQL 8.0, which is what
  lets `SchemaMigrationTest` actually run and the schema actually be verified.

- Flyway owns the schema (`db/migration`), with `ddl-auto=validate` in every
  profile so entity/schema drift fails at startup.
- A test suite exists under `src/test`. Indicator maths is specified by
  hand-computed vectors in `src/test/resources/vectors` and
  `docs/INDICATOR-SPEC.md`, written before the implementations.
- A `strategy` package with a concurrent scan engine, a Fair Value Gap
  detector, percentage-move and volume/open-interest rules, alerts with
  deduplication, and staged `TradePlan`s that never place an order.
- A Schwab `MarketDataProvider` adapter with OAuth token handling, rate
  limiting, retry and circuit breaking.
- A `csv` `MarketDataProvider` and a backtest harness (`bot/backtest`) that
  replays bars through the real rules with no lookahead — the rule sees
  `bars[0..i]` and enters at bar `i+1`'s open — scores outcomes in R after
  commission and slippage, resolves an intrabar stop-and-target bar **against**
  the position, and withholds win rate below a 30-trade floor. It holds no
  repository, so a replay cannot write to the account the live gate reads.

**Not done — do not assume these exist:**
- **The Schwab adapter has never run against the live API.** It is tested only
  against a stubbed server, because using it for real requires rotated
  credentials. Until then the default provider is in-memory and **no signal in
  this system comes from real market data.**
- **No market-stage gate.** Every signal is a setup signal; nothing enforces
  the exposure ceiling in the order path yet, so the first invariant in "the
  strategy this app implements" is documented but not applied. The
  live-trading gate that *does* exist is a different thing and is no substitute:
  it decides whether real money is reachable at all, not how much exposure the
  market stage permits. `OrderServiceImpl.placeOrder` is where both belong, and
  the ceiling must be built there before live trading is worth unlocking.
- **No live broker adapter.** Nothing can place a real order even in `LIVE`
  mode. The gate is the precondition for building one, not a switch that starts
  live trading.
- **No backtest *results*.** The harness exists (`bot/backtest`, specified in
  `docs/BACKTEST-SPEC.md` with hand-computed numbers written before the code),
  and a `csv` provider feeds it without credentials. But nothing has been run
  over real history, so **the rule thresholds are still untested guesses.**
  The harness also only exercises `PercentMoveRule` and the gap maths:
  `VolumeOpenInterestRule` and `HighValueOptionRule` need option chains with
  open interest, which bar files do not carry. A good equity backtest is not
  evidence about those two.
- **No indicator library** beyond the FVG detector, and no regime
  classification, setup detection, scale-out ladder or adaptive weighting.
- **No front end.** No `src/main/resources/static`, no dashboard.

**Standing security note:** Schwab API credentials — a `client_id`, a
`client_secret`, an OAuth access token and two authorization codes — were
committed to this repository, which was **public from 2025-08-03 until the
GitGuardian alert**, roughly fourteen months. They **must be treated as
compromised**: public GitHub is scraped continuously, so assume they were
collected.

History has since been rewritten (`git filter-repo --replace-text`) and the
values are redacted from all nine branches. Two things that rewrite did *not*
achieve, and neither should be forgotten:

- **GitHub's `refs/pull/*` refs still carry the original commits** (`f2c0c64`,
  `1bca748`). They are server-managed and cannot be force-pushed or deleted.
  Only a GitHub Support request to garbage-collect unreachable objects removes
  them; making the repository private is what denies anonymous access meanwhile.
- **Rotation at the Schwab developer portal is still outstanding.** It is the
  only action that makes the already-published values worthless. A purge limits
  further discovery; it does nothing about copies already taken.

Before this rewrite the note here claimed the credentials had been "removed from
the working tree". That was true of `master` but false of `sta`, whose tip still
held a live credential in `ThinkOrSwimClient.java`. Verify such claims against
every branch rather than the one you are standing on.

**Related branches — reference only, do not merge.** `main` and `sta` hold a
separate implementation of the same product (React + Tailwind frontend, FastAPI +
MongoDB backend, a substantial test suite, and a copy of this app's pre-reorg Java
code) on a lineage with no shared history with `master`. `master` is canonical.
Those branches are useful as sources — requirements, test cases, an existing
frontend — but their code is not merged here. The `em` branch is an unrelated
fitness application.
