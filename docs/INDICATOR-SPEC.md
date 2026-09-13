# Indicator specification

The expected values in `src/test/resources/vectors/` are written **before** the
indicators exist. This document records the conventions those vectors encode.

The reason for the ordering: every decision below has two defensible answers
that produce visibly different numbers. Deciding them here makes them
*specification*. Deciding them by writing the code first would freeze whatever
the first implementation happened to do, and a test written afterwards would
then assert that accident rather than the intent. `CLAUDE.md` states this as a
hard rule.

When the indicators are implemented (Phase 6), they are written against these
files red-to-green. A disagreement means the implementation is wrong, not the
fixture.

---

## Conventions

### Bar ordering
Oldest-first. Index `0` is the earliest bar, `size()-1` the most recent. This is
a `CLAUDE.md` invariant and every vector file follows it.

### Warm-up
An indicator with period `n` produces **no value** until it has `n` bars. The
vectors leave that cell **empty** rather than emitting `0`, `null`, or a partial
result. A zero would be indistinguishable from a genuine zero reading.

### Precision
All expected values are `DECIMAL` at **scale 4**, `HALF_UP`. Intermediate
arithmetic is carried at full precision; only the emitted value is rounded.
Rounding at each step accumulates drift that shows up several bars later.

### EMA seeding — **SMA-seeded**
The first EMA value is the **simple** average of the first `n` bars, emitted at
index `n-1`. Thereafter `EMA_i = (price_i − EMA_{i−1}) × k + EMA_{i−1}` with
`k = 2/(n+1)`.

The alternative — seeding with the first price — converges to the same curve but
differs materially for roughly `3n` bars. On a 50-period EMA that is most of a
trading year, which is long enough to change what the strategy buys.

### Wilder smoothing — **`1/n`, not `2/(n+1)`**
RSI and ATR use Wilder's smoothing:

```
value_i = (value_{i−1} × (n − 1) + input_i) / n
```

This is **not** the EMA multiplier. Wilder's `1/n` is equivalent to an EMA of
period `2n−1`, so using `2/(n+1)` by mistake produces a materially faster
indicator that still looks entirely plausible on a chart. This is the single
most common silent error in indicator code, which is why it is specified
explicitly and tested first.

Both seed from a simple average of the first `n` values.

### True range
`TR_i = max(high_i − low_i, |high_i − close_{i−1}|, |low_i − close_{i−1}|)`.

The second and third terms are what make TR gap-aware. Omitting them — using
only `high − low` — understates volatility exactly when it matters, and since
position size is `risk ÷ (trigger − stop)`, an understated ATR silently
oversizes positions.

The first bar has no previous close and therefore no TR.

### Weekly rollup
- Weeks start **Monday**.
- `open` is the first bar's open, `close` the last bar's close, `high`/`low` the
  extremes, `volume` the sum.
- A **partial trailing week is emitted**, not discarded — the current week is
  the one being traded.
- A **holiday-shortened week is a normal week**. It is not padded, and its
  shorter bar count is not treated as missing data. The vector carries a
  `bar_count` column so this stays visible.

### Contraction ratio
```
contraction(n) = ATR(n) over the last n bars ÷ ATR(n) over the n bars before
```
Below `1.0` means volatility is contracting, which `CLAUDE.md` names as the
entry condition. The fixture spans roughly 0.79 to 1.26 so both contraction and
expansion are exercised.

---

## Files

All vectors share one input series, `base_ohlcv.csv`, so a failure can be traced
to one set of bars.

| File | Contents |
|---|---|
| `base_ohlcv.csv` | The shared 30-bar input: date, OHLCV, oldest-first, weekdays only |
| `sma_10.csv` | Simple moving average, period 10 |
| `ema_10.csv` | Exponential moving average, period 10, SMA-seeded |
| `rsi_14_wilder.csv` | RSI, period 14, Wilder smoothing |
| `atr_14_wilder.csv` | ATR, period 14, Wilder smoothing, gap-aware TR |
| `weekly_rollup.csv` | Daily bars rolled to Monday-start weeks |
| `contraction_ratio_5.csv` | 5-bar contraction ratio |

## Provenance

The close series is the published StockCharts worked example for moving
averages; `sma_10.csv` and `ema_10.csv` agree with that published table
(SMA/EMA seed `22.2210`, next EMA `22.2081`).

RSI, ATR and the contraction ratio are **computed from Wilder's definitions** on
that same series by `docs/vectors-reference/generate_vectors.py`, not lifted from
a published table. Highs and lows are derived from the closes by a fixed offset,
so the series is synthetic in that respect but deterministic and re-derivable.

That generator is intentionally a **separate implementation** from the Java one.
Regenerating these files from the Java indicators would be circular — the tests
would then only prove the code agrees with itself. If a vector is ever shown to
be wrong, fix the generator and regenerate; do not edit a CSV by hand.
