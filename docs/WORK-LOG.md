# TCKR — Claude Code Work Log

Every story and task completed, with how it was done. Chronological.

Branch: `claude/production-trading-features-plan-h6301o`
Final state: **345 tests — 344 pass, 1 skipped** (Docker-gated)
Companion: `docs/PROJECT-OVERVIEW.md` — what the system *is*, as opposed to how
it came to be.

> Bugs found along the way are recorded in place, including the ones introduced
> and then caught, because how they were caught is the useful part.

---

## Phase 0 — Security hardening

### S0.1 Replace HTTP Basic with stateless JWT
**Done.** Added `JwtTokenProvider` (HS256, jjwt 0.11.5 — the last line built
against the `javax.*` baseline Boot 2.6 uses) and `JwtAuthenticationFilter`.
Access and refresh tokens are distinguished by a `type` claim so an access token
cannot be replayed at the refresh endpoint. Startup fails on a key under 32
bytes rather than issuing forgeable tokens. CSRF disabled with reasoning
recorded: auth is a bearer token in a header, never a cookie, so there is no
CSRF vector — and a note that it must be re-enabled if auth ever moves to a
cookie.

### S0.2 Login lockout and rate limiting
**Done.** Bucket4j token-bucket limiting per IP on auth endpoints, plus attempt
counting with a lockout window. Unknown user and wrong password fail identically,
so usernames cannot be enumerated.

**Problem hit:** `com.bucket4j:bucket4j-core:7.6.0` did not resolve — that
version lives under `com.github.vladimir-bukhtoyarov`. Switched to
`com.bucket4j:bucket4j-core:8.10.1`.

### S0.3 Error-response and log hygiene
**Done.** `GlobalExceptionHandler` maps exceptions centrally. Controllers no
longer catch and echo `e.getMessage()`. Unanticipated failures return a
correlation id with the cause logged server-side. SQL bind-parameter logging
confined to `dev`, since bind values carry passwords and PII. `User.toString()`
no longer prints the password hash.

### S0.4 Leaked Schwab credentials — decision recorded
**Done (decision), outstanding (action).** Credentials were found committed.
Offered rotate-now versus history-rewrite-now; the choice was **"rotate first,
purge later"**, so no history rewrite was performed at this stage and the
standing note was added to `CLAUDE.md`.

---

## Phase 1 — Boot blockers and dead code

### S1.1 Fix boot blockers and dead-code defects
**Done.** Reorganised into package-by-feature. Fixed `Stock.last` holding
per-instance data in a `static` field — which silently returns another ticker's
numbers.

---

## Phase 2 — Core domain model

### S2.1 Order and execution state machine
**Done.** `Order` + `OrderExecution` with a table of legal transitions, so an
illegal move throws rather than being recorded. Partial fills accumulate.

### S2.2 PnL and account settlement
**Done.** `PnLCalculator` books realized P&L with an oversell guard.
`Account`/`LedgerEntry` check buying power on BUY and credit proceeds on SELL.

### S2.3 Risk-based position sizing
**Done.** `PositionSizer`: `shares = (equity × riskPercent) ÷ (trigger − stop)`,
floored so the risk budget is never exceeded. Conviction is not an input and
there is no parameter through which it could become one. Tested so that a wider
stop demonstrably buys fewer shares while risking the same amount.

### S2.4 Close the userId authorization hole
**Done.** Every trading route derived identity from client-supplied data. Added
`CurrentUserService` as the only sanctioned source of caller identity, removed
`userId` from request DTOs, and passed it to services as an explicit parameter
so the compiler catches a route that forgets. Reads of another user's record
return not-found rather than forbidden, so ids cannot be probed.

---

## Governance

### S-G.1 Reconcile two conflicting planning documents
**Done.** Merged into committed `CLAUDE.md` (rules that outlive a phase) and
`PHASES.md` (what to build next), with invariants tagged `[enforced]` /
`[partial]` / `[future]` so a future rule is not mistaken for a guarantee.

**Found:** the exposure-ceiling line read positionally gives *Basing* full
exposure and *Advancing* 30% — backwards for a momentum strategy. Raised;
intent confirmed as Advancing 1.00, Basing 0.30, Topping 0.25, Declining 0.00.

**Found:** `guide.md` did not exist. Proven by filesystem sweep, code search
across all 22 repositories, and MD5 — the two attached planning files were
byte-identical (`e77f7f1…`).

### S-G.2 Correct false claims in CLAUDE.md
**Done.** The "Current state" section claimed no tests, no migrations and no
strategy engine, all of which existed. Corrected twice.

---

## Phase 3 — Schema ownership

### S3.1 Give Flyway ownership of the schema
**Done.** `V1__baseline_schema.sql`, `ddl-auto=validate` in *every* profile so
drift fails at startup rather than as a runtime query error. Legacy SQL moved to
`docs/legacy-schema/` with a README documenting three conflicting prior
definitions.

**Found — entity/schema drift:** `Trade`/`Position` mapped a literal column
`stockTicker` while the SQL said `stock_ticker`; `Order`/`Trade`/`Options`
joined via `referencedColumnName = "stockTicker"`, a column that does not
exist. **Two of those broken joins were ones I had introduced myself**, and were
flagged as such. `prod` already set `ddl-auto=validate`, so production could not
have booted.

**Problem hit:** `flyway-mysql` does not exist at Boot-managed 8.0.5 (it appears
at 8.2.1). Removed it — MySQL support ships inside `flyway-core` at that version.

---

## Phase 4 — Test foundation

### S4.1 Testcontainers, not H2
**Done.** Imported `testcontainers-bom:1.19.8` (Boot 2.6.6 does not manage those
versions). MySQL rather than H2 deliberately: H2 does not reproduce MySQL's
`ENUM` columns, FK semantics or dialect behaviour, so it can pass while
production fails.

**Found:** `SchemaMigrationIT` was **silently never running** — Surefire matches
only `*Test` patterns and `*IT` needs Failsafe, which was not configured.
Renamed to `SchemaMigrationTest` so it visibly skips instead of silently
vanishing.

### S4.2 Specify indicator maths before implementing it
**Done.** Wrote `docs/INDICATOR-SPEC.md` and hand-computed vectors in
`src/test/resources/vectors` — SMA(10), EMA(10), RSI(14), ATR(14), contraction
ratio, weekly rollup. The generator was written as a deliberately separate
implementation to avoid circularity. `IndicatorVectorFixtureTest` validates the
fixtures themselves: rectangular, oldest-first, warm-up empty, scale 4.

---

## Phase 5 — Strategy skeleton

### S5.1 Concurrent scan engine
**Done.** `StrategyEngine` with a bounded owned pool (not `parallelStream`,
which runs on the shared common ForkJoinPool), one task per ticker, a semaphore
around provider calls, a per-task deadline, and sorted deterministic output so a
parallel scan and a serial one agree. Failures surface in
`ScanResult.failures()`, never swallowed. The order path was kept explicitly
single-threaded: an exposure ceiling is a global constraint and two threads
checking capacity at once both see room for one more.

### S5.2 Fair Value Gap detector
**Done.** Three-bar imbalance with **strict** inequality, so a touching bar is
not a gap.

**Bug found in my own test, not the code:** `multipleGaps` expected 2 gaps and
got 3. The detector was right — every rolling 3-bar window is a candidate. Fixed
the fixture and added `scansOverlappingWindows` to pin the behaviour.

### S5.3 Signal rules, alerts and staged plans
**Done.** `PercentMoveRule` with a configurable baseline, so "up 4% from
yesterday" and "up 2% in the last half hour" are one code path with one set of
tests. `VolumeOpenInterestRule` with an absolute volume floor.
`HighValueOptionRule`. `AlertService` with per-user, per-condition cooldown
deduplication — without it a five-minute scan re-raises an unchanged condition
twelve times an hour and trains its reader to ignore the feed. `TradePlan`s stage
proposals and never place an order.

---

## Phase 5b — Schwab market-data adapter

### S5b.1 Build the real provider adapter
**Done.** OAuth token handling, Resilience4j rate limiting, retry with
exponential backoff and jitter, circuit breaking. WireMock contract tests so the
failure paths are exercised without a key.

Schwab specifics handled: no 1-hour frequency, so `BarAggregator` builds hourly
bars from 30-minute pairs, aligned from the series start so hours are
session-aligned (09:30–10:30) with the trailing partial emitted not dropped;
`OptionQuote` carries two timestamps because open interest publishes once daily;
`previousSessionClose()` derives `openInterestAsOf`; `decimalValue()` not
`asDouble()` so prices stay exact.

**Credential-shredding defect found by a failing test.** A single-flight refresh
test expected 1 refresh and saw 24. Part of it was my fake issuing clock-fixed
tokens — but it exposed a real defect: a token born expired caused a refresh on
*every call*, and since Schwab refresh tokens are one-time use, each iteration
spent one. Added a `MINIMUM_REFRESH_INTERVAL` guard with a dedicated test, and
made the refresh token durable **before** being published.

**Problems hit:** a hand-implemented `JpaRepository` fake would not compile
across Spring Data versions — replaced with a Mockito mock backed by a holder.
A stray non-English word in a commit message was amended out with
`--force-with-lease`.

### S5b.2 Recover an unmerged commit
**Done.** PR #2 merged capturing only the first commit; the Phase 2 commit was
left unmerged. Recovered by cherry-picking onto `master`. Later merged PR #7 so a
single `git pull` retrieves everything.

---

## Phase 6 — Refresh token rotation

### S6.1 Make refresh tokens single-use
**Done.** Traced from the commented-out `TokenController` prose sitting in two
controllers: it listed five rotation steps and `AuthController` implemented four.
The missing one — "invalidate the previous refresh token" — was unimplemented,
confirmed by grep finding no `invalidat|revoke|blacklist|jti` anywhere outside
those comments. With a 7-day TTL a stolen token worked for a week alongside the
legitimate holder, neither seeing a sign of the other.

Added `V4` plus `RefreshTokenStore`. The token itself is never stored — only its
`jti`, since the signature proves authenticity. Redemption is a conditional
`UPDATE ... WHERE used_at IS NULL`, so InnoDB's row lock picks the winner. A
second redemption revokes the whole family per RFC 9700.

`RefreshTokenStore` returns an outcome rather than throwing, because a runtime
exception raised inside the transaction that performed the revocation would roll
it back and leave the stolen token live.

**Pattern already in the repo:** `SchwabAuthService` already treated Schwab's
refresh tokens as one-time use. It had simply never been applied to our own.

**Also moved** the `Clock` bean from `marketdata/config` to a new
`common/config/CoreConfig` — `common` may not depend on a feature package, and
`common/security` now needed it.

### S6.2 Salvage value from dead comments, then plan their removal
**Done.** Audited ~160 commented lines across two controllers. Found they were
**byte-identical** between files (md5 `c8ad852`). Transcribed the one thing with
value — a `TradingBotApp` sketch naming missing indicator scope — into
`PHASES.md`, after checking each item: `OrderHistory` and `PositionManager` were
already superseded by `TradeService`, leaving only fundamentals and order flow as
real gaps.

**Found:** three routes claimed to execute trades and did nothing.
`/trade/stock` and `/trade/options` called service methods with **empty bodies**
and returned HTTP 200 "Trade executed successfully"; `/api/stock` returned
`null`, which Spring renders as 200 with an empty body. All three bypassed
`OrderService` entirely — no state machine, no buying-power check, no ledger.
Zero tests referenced them.

**Found:** `StockStrategies` has **no implementation anywhere** —
`StockStrategiesImpl` has no `implements` clause despite the name. Jackson
cannot instantiate an abstract type, so `POST /trade/options` threw at
`@RequestBody` binding on *every* request, before reaching the empty service
method. It never worked, in a second independent way.

**Found:** `StockTradeRequest` still carried a `userId` field — a live template
for the mistake the authorization invariant forbids.

**Pushed back on my own plan.** Challenged on deleting all eight files, I
narrowed it: `StockController` should be **repurposed** (there is no endpoint
anywhere to read market data, and it sits in the right package under the right
name), and `StockStrategies` left alone since `PHASES.md` already schedules that
decision. Deletion of the remaining five is **approved but blocked** by the
permission classifier.

---

## Phase 7 — Containerisation

### S7.1 Docker image and local MySQL stack
**Done.** Multi-stage Dockerfile — Maven and the JDK stay in the build stage;
the runtime ships a JRE and the extracted layered jar. Dependencies resolve in a
pom-keyed layer so editing a Java file does not re-download the tree. Non-root
user. Heap sized by `MaxRAMPercentage` so changing the container's memory limit
needs no rebuild. `docker compose` with MySQL 8.0, a healthcheck, and
`depends_on: service_healthy` so Flyway does not race an unready database.

**Two pre-existing blockers found and fixed first:**

1. **`spring-boot-maven-plugin` was never declared.** `mvn package` produced a
   plain jar with no `Main-Class` and no bundled dependencies — it could not be
   run at all. Added with layers enabled and a stable `finalName`.
2. **`spring.datasource.url` hardcoded `127.0.0.1`**, unreachable from a sibling
   container. Host/port/database now come from the environment, defaulting to the
   old values so non-Docker runs are unchanged.

Verified here: the jar builds, its manifest names `JarLauncher` and
`com.moneyteam.Television`, and `layertools` emits exactly the four layers the
Dockerfile copies. Actuator added for one reason — Compose needs a real
readiness signal — with only `/actuator/health` exposed.

`.dockerignore` excludes `.git` and `.env`: image layers are as durable and
copyable as git history, which is how the Schwab credentials became a standing
problem.

---

## Phase 8 — Personal bot: identity and the gate

### S8.1 Single-owner bot identity
**Done.** A scheduled scan has no authenticated principal, so `BotIdentity`
resolves one owner from `app.bot.owner-username` — the bot's equivalent of
`CurrentUserService`, and like it a source the client cannot influence. **No DTO
gained a `userId`**; services still take it as an explicit parameter.

`BotOwnerInitializer` seeds the owner's funded paper account idempotently, and
**deliberately will not create the user** — that would mean inventing a
password, and a password chosen by the application is a default credential. It
fails at startup with the registration command instead.

### S8.2 The live-trading gate
**Done.** `V5` plus `LiveTradingGate`, holding *armed* and *unlocked* as two
separate facts so an arithmetic error in the equity calculation can never by
itself trade real money. Unlock requires echoing the exact target back.

**Corrected two errors in my own approved plan:**

1. The plan said `assertCanOpenRisk()` should "throw when mode is PAPER" — which
   would have made paper trading impossible. The gate guards *real-money* orders;
   paper is the normal path.
2. The plan's cash-only equity reading was wrong: a fully invested account would
   show almost nothing and could never arm, however well it was doing. Changed to
   cash plus open positions **at cost** — which needs no market data and counts
   only booked money, so an unrealized spike cannot arm the gate.

**A SELL is never gated**, even with no configuration at all. Blocking a close
traps an open position, which is worse than no gate.

**Found:** `BotConfigRepository` originally exposed `findSingleton()` as a
`default` method delegating to a derived finder. Mockito does not execute default
methods on a mock, so it returned empty however the delegate was stubbed — a trap
for any future test. Made it a `@Query` method.

---

## Phase 9 — GitGuardian remediation

### S9.1 Investigate the alert
**Done.** My push added **no real secret** — every `.env.example` value is blank
and `HEAD` held only placeholders. The real finding was pre-existing: **five real
credentials** in git history (`client_id`, `client_secret`, an OAuth access token,
two authorization codes) in a repository **public since 2025-08-03 — roughly
fourteen months**.

**Far wider than recorded:** present on **8 of 9 branches**, not just `master` —
every `claude/*` branch plus `main` and `sta`, where they sit in Python sources.
Only `em` was clean.

**Found:** `CLAUDE.md` claimed the credentials were "removed from the working
tree". True of `master`, **false of `sta`**, whose tip still held a live
credential in `ThinkOrSwimClient.java`. Discovered because the rewrite changed
`sta`'s tip tree while every other branch tip stayed byte-identical. Note
corrected, with the lesson: verify such claims against every branch.

### S9.2 Remove the literals I had introduced
**Done.** Two `SECRET` constants in test files I had added, plus pre-existing
literals in `application-test.properties` and `SchemaMigrationTest`. None was
ever a credential, but each would have produced a false positive on every future
push — and a repository whose alerts are mostly noise is one where the real alert
gets waved through. Replaced with `support/TestKeys` (seeded from a label, so
reproducible) and `${random.value}`. **Deliberately not suppressed** via a
`.gitguardian.yaml` exclusion: silencing a detector is the wrong reflex and the
fix was smaller.

### S9.3 Purge history
**Done.** Took a full mirror backup first. Used `git filter-repo
--replace-text`, not `--invert-paths`, so values are redacted in place and no
file is deleted — `main` and `sta` hold a separate working implementation and
deleting `backend/server.py` would have amputated it.

**A trap caught before it did damage:** the first extraction pass also matched
`os.environ.get` from the Python sources. Had that reached the replacement list
it would have rewritten `os.environ.get` to `***REMOVED-CREDENTIAL***` across
both branches, silently breaking them. Filtered out; the final list was 5 values.

Verified on a disposable copy *before* pushing, and with the right method: a
first check using `git grep <ref>` was **too weak** — it only searches ref tips,
and these secrets live in historical commits, so it would have given a
misleading pass. Re-verified with `git log --all -S` across full history: 2–3
commits per credential in the backup, **0** in the rewrite. Tip trees of
`master`, `main`, `em` and the feature branch stayed byte-identical.

Force-pushed 8 branches. Then re-cloned the remote and verified again — and found
the residual below.

### S9.4 The residual I cannot fix
**Reported.** Commits `f2c0c64` and `1bca748` are **still on GitHub**, reachable
from `refs/pull/2/head` through `refs/pull/7/head`. Checked per-ref: no branch
reaches them, only those six PR refs. GitHub owns those refs — they cannot be
force-pushed or deleted, and closing the PRs does not remove them.

Delivered a remediation write-up with a ready-to-send GitHub Support request.
The credential-value file was shredded; the mirror backup retained.

**Still outstanding and only the owner can do it:** rotate at the Schwab portal,
make the repository private (the only same-day mitigation for those refs), send
the Support request.

---

## Phase 10 — Backtest harness

### S10.1 CSV bars and the harness
**Done.** `CsvMarketDataProvider` behind the existing port — the one data source
needing no credentials, which is why it came first while the rotation was
pending. `docs/BACKTEST-SPEC.md` written **before** the code, with every number
hand-computed.

Three decisions a backtest can quietly make in its own favour, settled
explicitly: entry at bar `i+1`'s **open** (entering at the signal bar's close is
the classic lookahead bug); a bar holding both stop and target scored a **loss**
(OHLC cannot say which came first, and resolving it the other way turns losing
systems into winners on paper); slippage charged on **both** sides.

No-lookahead is **structural** — the rule receives `bars.subList(0, i+1)`, so a
future price is absent from the object rather than merely unused.

**Trap documented on the method that causes it:** `getPreviousClose()` on a CSV
file returns the *last row*, which is a future price at every replay step but
the last. Lookahead arriving through an innocent-looking interface. The runner
derives it from its own window instead.

**Decided against routing through `StrategyEngine`**, for two concrete reasons:
it stamps contexts with `Instant.now()` so historical signals would get
wall-clock times, and it exists to parallelise whereas a backtest must be
deterministic.

The runner holds **no repository**, asserted structurally rather than by
observing no writes during one run — a replay that booked fills into `Account`
would feed the live-trading gate.

Reports **withhold** win rate and expectancy below 30 closed trades, returning
null rather than the figure with a flag, so a caller must handle the absence.

**Scope limit stated plainly:** only `PercentMoveRule` and the gap maths are
exercised. The two option rules need chains with open interest that bar files do
not carry, so a good equity backtest is not evidence about them.

---

## Phase 11 — Alpaca paper trading

### S11.1 Establish that TOS paperMoney is unreachable
**Done.** Verified across sources: paperMoney lives inside the thinkorswim
platform, Schwab's Trader API places only real orders on funded accounts, and its
developer sandbox covers authentication and token refresh against synthetic
data — there is no simulated-fill mode and no paper endpoint.

### S11.2 Broker port and Alpaca paper adapter
**Done.** `BrokerPort` with `InMemoryBroker` (default and test double) and
`AlpacaBroker` against the **paper host only**. No live URL exists anywhere in
the codebase, and Alpaca issues separate key pairs per environment so a paper key
is refused by the live host — real money out of reach for two independent reasons.

**Order placement is deliberately not retried** while reads are: retrying an
order whose response was lost is how one intended position becomes two. The
client order id is the recovery path, and `InMemoryBroker` honours it as an
idempotency key so that path is tested.

**An unrecognised broker status maps to `OPEN`, not `REJECTED`** — a new state in
a future API version must not read as a refusal, which would make the reconciler
abandon a live order.

**Found:** Alpaca sends order and account numbers as JSON **strings**, where
Jackson's `decimalValue()` returns zero for a textual node. Parsing through it
would have produced a silent zero quantity. Parsed from text instead, with a
loud failure on a malformed number. Bar prices *are* real JSON numbers and do use
`decimalValue()`, keeping the exact decimal that gap detection needs.

### S11.3 Idempotent fill reconciliation
**Done.** `FillReconciler` books **deltas, not totals**: 30% then 80% books 30
then 50, and a repeat poll books nothing. Our `Account` feeds the gate, so a
double-booked fill could arm it on money never made. A broker reporting *less*
than already booked is logged and ignored; a booking that throws leaves the delta
for the next poll rather than dropping it.

**Limit stated in the class:** the booked-quantity map is in memory, so a restart
mid-fill would re-book from zero. Acceptable for paper, **not for live**; the fix
is a column on `Order`.

### S11.4 Scheduled runner
**Done.** `BotRunner` does not exist as a bean unless `app.bot.enabled=true`.
Checks the market clock first so a closed market costs one cheap call rather than
a scan and a run of rejected orders. Places through `OrderService`, not the
broker directly, so the gate applies — and so the market-stage ceiling will too.

**Flagged, not hidden:** the runner's stop is a placeholder (trigger less the
threshold percentage) rather than a structural level, made explicit in the code
so it cannot be mistaken for finished.

**Two free-tier limits recorded in CLAUDE.md, the README and the provider's own
log line**, because they change what may be concluded: the feed is IEX, a small
share of consolidated volume, so **any volume threshold calibrated on it is
calibrated against a few percent of what traded**; and historical bars lag ~15
minutes, which is why the runner is daily-only.

---

## Phase 12 — Trade journal

### S12.1 Learn what words leave out
**Done.** Built for a trader who describes setups visually. The premise: you
cannot learn what someone cannot articulate by asking them to articulate it. So
nothing asks for a setup to be described — it records the fill, a
`ContextSnapshot` of the market at that instant, and the marked-up chart, then
replays the encoded rules over each snapshot. Trades taken while every rule
stayed silent are the articulation gap, located by comparison.

Shipped the comparison **first**, deliberately: it needs no training data, no
sample size and no options vendor, and is useful from the first few trades, where
a model fitted to twenty option trades would mostly describe noise.

**The property everything rests on:** a snapshot must replay to the *same signal*
the live rules produced — asserted on the signal's own evidence map, not field
equality. A lossy reconstruction would not fail; it would report disagreements
that are artefacts and read exactly like findings.

Four decisions recorded:

- **Fidelity is a column.** An imported fill has no chain from its fill moment.
  Analyses are scoped to one tier, and `RuleBook` strips chain features from
  entries whose tier lacks them.
- **Judgement is separate from R**, and `judge()` cannot see an outcome. A winner
  the trader would not repeat is what a P&L-fitted model learns wrongly from.
- **Absence is never compliance.** A rule whose feature is missing reports
  *uncheckable*. "Held on all 0 fills" would read as reassurance.
- **An unknown feature is refused at parse time.** `close > ema21` is exactly the
  rule someone would state, and there was no indicator library — so it fails
  loudly with the vocabulary listed.

**Uploads treated as untrusted throughout:** type from magic bytes not the
declared type or extension, generated stored names so nothing client-supplied
reaches a path, size capped before any write, root containment checked after
normalisation.

**Flagged:** EXIF is not stripped — the fix needs an imaging dependency the
project does not have.

---

## Phase 13 — Runnable demo

### S13.1 Pipeline walkthrough
**Done.** `scripts/demo.sh` + `PipelineDemo` walk the analytical core end to end
using the **real production classes** — a demo that reimplemented any of it would
demonstrate the demo. Seven sections: bars, signals, gaps, sizing, backtest,
broker idempotency, articulation gap. Needs no database, credentials or Docker.

**Declined to boot on H2 to fake a full demo.** `CLAUDE.md` is explicit that H2
can pass where MySQL fails, the migrations use `ENGINE=InnoDB`, `TIMESTAMP(6)`
and `ENUM`, and a green H2 boot would imply the schema is verified when it has
never been applied to MySQL.

**Declined to fake `LiveTradingGate`.** It reads three repositories, and faking
those so a demo could print "real money is locked" would be precisely the wrong
thing to fake.

The generated series' R figures are labelled **not a backtest result** in the
runner's own output, twice, so a screenshot cannot be mistaken for evidence.

### S13.2 A bug the demo caught on first run
**Done.** `BacktestReport.summary()` printed its own format string verbatim —
`%d trade(s) over %d bars, total %s R` — because `.formatted()` binds tighter
than `+`, so it applied to the last literal of a concatenation and left the first
half unsubstituted. Fixed by parenthesising, with a comment saying why the
brackets are load-bearing.

**How it survived:** the existing test asserted the summary contained
`"withheld"` — and that word lived in the half that *was* formatted. The test
passed throughout while the output was broken. The new test asserts on the
substituted values and the absence of `%d`/`%s`.

---

## Phase 14 — Indicator library

### S14.1 Implement the specified indicators
**Done.** Filled in a spec written months earlier: `IndicatorSeries`,
`MovingAverages` (SMA, EMA), `WilderSmoothing`, `TrueRange`,
`AverageTrueRange`, `RelativeStrengthIndex` — each asserted cell-for-cell
against its pre-written vector column.

**The vectors immediately caught a real bug.** RSI came out at **74.2377**
against an expected **74.2222**. Rather than assume the fixture was wrong, I
recomputed independently in Python: the vector was right and **my implementation
was wrong**.

The cause was exactly what the spec forbids. RSI divides one smoothed average by
another, and both were being read back out of `IndicatorSeries` **already rounded
to scale 4** — so the ratio shifted enough to move the final value. Added
`WilderSmoothing.smoothRaw` for callers that feed a result into further
arithmetic; ATR has no such step and still uses the emitted form.

That is the whole argument for writing expected values first. The implementation
looked right, produced plausible numbers, and stayed inside RSI's 0..100 bounds.
Nothing but a number computed beforehand would have caught it.

Two settled decisions pinned by their own tests rather than left implied:

- **EMA is SMA-seeded** — its first value equals SMA at the same index. Seeding
  from the first price converges to the same curve but differs for roughly `3n`
  bars, which on a 50-period EMA is most of a trading year.
- **Wilder's `1/n` is not the EMA's `2/(n+1)`.** Since Wilder(n) equals an EMA of
  period `2n−1`, the wrong constant yields a materially faster indicator that
  looks plausible on a chart and breaks no bound — which the spec calls the
  commonest silent error in indicator code. One implementation, one test.

**Warm-up is `Optional.empty()`, enforced by the return type.** A flat series
whose SMA really is `0.00` is tested to confirm a genuine zero stays
distinguishable from a warm-up cell.

---

## In progress

### S15.1 Market-stage exposure ceiling
**Designed, not built.** The next and most significant piece: classify
Basing / Advancing / Topping / Declining and enforce the ceiling as a hard cap in
`OrderServiceImpl.placeOrder` — the single choke point, which already carries a
comment saying this belongs there.

**Flagged before building:** the ceiling line in `CLAUDE.md` read positionally
gives *Basing* 1.00 and *Advancing* 0.30, which is inverted for momentum. The
confirmed mapping is **Advancing 1.00, Basing 0.30, Topping 0.25, Declining
0.00**, and the document needs correcting.

Design decisions taken: the MA slope band measured in **ATR units** rather than
price or percent, so "flat" does not change meaning as the index re-rates;
`UNKNOWN` carrying ceiling **0.00**, since not knowing the regime is a reason to
open no new risk; and **`LiveTradingGate.unlock()` refusing while the ceiling is
disabled**, so the feature flag defaulting off cannot make it decorative.

---

## Blocked on the owner

| Item | Why it is blocked |
|---|---|
| Rotate the Schwab credentials | Only the account holder can, at the developer portal. The only action that makes published values worthless. |
| Make the repository private | The only same-day mitigation for the `refs/pull/*` residual. |
| Send the GitHub Support GC request | Goes from the owner's account. Draft delivered. |
| Delete the five dead files | `git rm` refused by the permission classifier as irreversible local destruction; not worked around. |
| First real `docker compose up` | No Docker daemon or MySQL in this container. |
| Pick an options data vendor | Needed for chain context on imported journal fills. |
