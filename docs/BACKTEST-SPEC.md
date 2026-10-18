# Backtest specification

Written **before** the implementation, like `INDICATOR-SPEC.md`. Every number
below is computed by hand. The implementation is correct when it reproduces
these, not the other way round.

The point of writing it first is that the ambiguous decisions — when entry
happens, what a bar that touches both stop and target means, whether costs come
off the numerator or the denominator — get settled as specification. Settled
afterwards, they get frozen as whatever the first implementation happened to do,
and a backtest that quietly resolves ambiguity in its own favour is worse than
no backtest.

## Timeline rules

| Event | When |
|---|---|
| Rules evaluated on bar `i` | after bar `i` has closed |
| Context visible to the rule | `bars[0 .. i]` only |
| Entry | bar `i+1`'s **open** |
| Exit checks begin | bar `i+1` |

A signal computed from bar `i`'s close cannot be acted on at that close — the
close is only known once the bar is over. Entering at bar `i`'s close is the
classic lookahead bug and inflates results for any momentum rule.

The visible window is enforced structurally: the rule is handed
`series.subList(0, i + 1)`, so later bars are not merely ignored, they are
absent from the object.

### `previousClose` during replay

`PercentMoveRule` with `PREVIOUS_CLOSE` reads `context.previousClose()`. During
replay this is **`bars[i-1].close()`**, computed by the runner from the
truncated series.

It is deliberately *not* taken from `MarketDataProvider.getPreviousClose()`:
for a CSV file that returns the last bar in the file, which at step `i` is a
future price. That would be lookahead entering through the back door of an
interface that looks innocent.

## Stop, target, size

- **Stop** = the signal bar's low, `bars[i].low()`.
- **Planned risk per share** `R$` = `trigger − stop`, fixed at plan time.
- **Target** = `trigger + 2 × R$` (via `PositionSizer.firstTarget`, 2R).
- **Shares** = `PositionSizer.sharesForRisk(equity, riskPercent, trigger, stop)`.

`R$` is fixed at plan time and is the denominator for every R figure. It is not
recomputed from the actual fill: a trader sizes from the plan, so R has to be
measured against the plan or it stops meaning "multiples of what I risked".

## Intrabar ambiguity resolves against the position

If a bar's `low ≤ stop` **and** its `high ≥ target`, OHLC does not record which
came first. **The stop is taken.**

This is not conservatism for its own sake. Resolving it the other way turns
losing systems into winning ones on paper, because the ambiguous bars are
exactly the volatile ones a momentum rule produces most of.

## Costs

- `slippageBps` moves the fill against the position on **both** sides: a long
  buys higher and sells lower.
- `commissionPerShare` is charged on entry and on exit.

```
entryFill = trigger  × (1 + slippageBps/10000)
exitFill  = exitPx   × (1 − slippageBps/10000)
realised  = exitFill − entryFill − 2 × commissionPerShare
R         = realised ÷ R$
```

All money to scale 4, `HALF_UP`, per the money rule in `CLAUDE.md`.

## Worked fixture — `src/test/resources/backtest/TEST_D1.csv`

Daily bars. Rule: `PercentMoveRule`, up-only, 4% from previous close, `D1`.
Equity 100,000, risk 1%.

| i | openTime | open | high | low | close |
|---|---|---|---|---|---|
| 0 | 2026-01-02 | 100.00 | 101.00 | 99.00 | 100.00 |
| 1 | 2026-01-05 | 100.00 | 106.00 | 99.50 | 105.00 |
| 2 | 2026-01-06 | 105.50 | 112.00 | 105.00 | 111.00 |
| 3 | 2026-01-07 | 111.00 | 118.00 | 110.00 | 117.00 |

**Signal at i=1.** `previousClose` = `bars[0].close` = 100.00.
`(105.00 − 100.00) ÷ 100.00 × 100 = 5.0000%` ≥ 4% → fires.

No signal at i=0: there is no `bars[-1]`, so no baseline.

**Entry at i=2's open = 105.50.**

```
stop        = bars[1].low            = 99.50
R$          = 105.50 − 99.50         = 6.00
target      = 105.50 + 2 × 6.00      = 117.50
riskBudget  = 100000 × 1%            = 1000.00
shares      = floor(1000 ÷ 6.00)     = 166
```

**Exit scan.** Bar 2: high 112.00 < 117.50, low 105.00 > 99.50 → still open.
Bar 3: high 118.00 ≥ 117.50 → target. Low 110.00 > 99.50, so the bar is not
ambiguous and the target stands.

**Exit at target, 117.50.**

### Expected results

Zero costs:

```
realised = 117.50 − 105.50 = 12.00
R        = 12.00 ÷ 6.00    = 2.0000
```

10 bps slippage, zero commission:

```
entryFill = 105.50 × 1.001 = 105.6055
exitFill  = 117.50 × 0.999 = 117.3825
realised  = 117.3825 − 105.6055 = 11.7770
R         = 11.7770 ÷ 6.00       = 1.9628   (1.96283̄ → HALF_UP at scale 4)
```

So costs strictly reduce R, which is the assertion worth having.

### Stop case

Replace bar 3 with `open 111.00, high 118.00, low 99.00, close 100.00`. Now
`low 99.00 ≤ 99.50` and `high 118.00 ≥ 117.50` — ambiguous, so the stop is
taken:

```
realised = 99.50 − 105.50 = −6.00
R        = −1.0000
```

## Low-sample abstention

Below `minimumTrades` (default **30**) closed trades, the report returns the
trade count and total R but **abstains** from win rate and expectancy, flagging
`lowSample = true`.

A win rate from eight trades is not an estimate of anything, and quoting one
invites sizing up on noise. The fixture above produces a single trade, so every
report in the test suite is expected to be flagged — that is the behaviour, not
a shortcoming of the fixture.

## Out of scope, and why

Only `PercentMoveRule` is exercised. `VolumeOpenInterestRule` needs
`optionChain()` and `HighValueOptionRule` needs `optionChain()` and
`optionBars()`; equity OHLCV files contain neither, and historical option chains
carrying open interest are not freely available.

**So this harness says nothing about whether the volume-to-open-interest
threshold or the option-chart FVG confluence works.** Those stay untested
guesses until there is an option data source. Do not read a good equity
backtest as validation of them.
