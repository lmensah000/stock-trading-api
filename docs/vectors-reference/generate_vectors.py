#!/usr/bin/env python3
"""
Reference implementation used to generate the indicator vectors in
src/test/resources/vectors/.

This exists so the expected values are auditable and reproducible rather than
magic numbers. It is deliberately a SEPARATE implementation from the Java one:
if the Java indicators are later written to match these files and they agree,
that agreement means something. Regenerating from the Java implementation
itself would be circular and would defeat the purpose.

Conventions implemented here are specified in docs/INDICATOR-SPEC.md.

Usage:  python3 docs/vectors-reference/generate_vectors.py
"""
import csv, os
from datetime import date, timedelta
from decimal import Decimal, ROUND_HALF_UP

OUT = os.path.join(os.path.dirname(__file__), "..", "..",
                   "src", "test", "resources", "vectors")
OUT = os.path.normpath(OUT)
os.makedirs(OUT, exist_ok=True)

SCALE = Decimal("0.0001")
def q(x):
    return Decimal(x).quantize(SCALE, rounding=ROUND_HALF_UP)

# ---------------------------------------------------------------- input
# Closes are the published StockCharts worked example used for SMA/EMA/RSI.
CLOSES = [
    22.27, 22.19, 22.08, 22.17, 22.18, 22.13, 22.23, 22.43, 22.24, 22.29,
    22.15, 22.39, 22.38, 22.61, 23.36, 24.05, 23.75, 23.83, 23.95, 23.63,
    23.82, 23.87, 23.65, 23.19, 23.10, 23.33, 22.68, 23.10, 22.40, 22.17,
]
# High/low derived deterministically so ATR has a real range to work with.
HIGHS = [round(c + 0.35, 2) for c in CLOSES]
LOWS  = [round(c - 0.30, 2) for c in CLOSES]
VOLS  = [1000 + 25 * i for i in range(len(CLOSES))]

# Trading days only: start Mon 2024-01-01, skip weekends. Oldest-first.
def trading_days(n, start=date(2024, 1, 1)):
    out, d = [], start
    while len(out) < n:
        if d.weekday() < 5:
            out.append(d)
        d += timedelta(days=1)
    return out
DATES = trading_days(len(CLOSES))

def write(name, header, rows):
    p = os.path.join(OUT, name)
    with open(p, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(header)
        w.writerows(rows)
    print(f"  {name}: {len(rows)} rows")

# ---------------------------------------------------------------- base
write("base_ohlcv.csv", ["date", "open", "high", "low", "close", "volume"],
      [[DATES[i].isoformat(), f"{CLOSES[i]:.2f}", f"{HIGHS[i]:.2f}",
        f"{LOWS[i]:.2f}", f"{CLOSES[i]:.2f}", VOLS[i]]
       for i in range(len(CLOSES))])

# ---------------------------------------------------------------- SMA
def sma(values, period):
    out = [None] * len(values)
    for i in range(period - 1, len(values)):
        out[i] = q(sum(Decimal(str(v)) for v in values[i - period + 1:i + 1]) / period)
    return out

# ---------------------------------------------------------------- EMA
def ema(values, period):
    """SMA-seeded: the first EMA value is the SMA of the first `period` bars."""
    out = [None] * len(values)
    k = Decimal(2) / Decimal(period + 1)
    seed = sum(Decimal(str(v)) for v in values[:period]) / period
    out[period - 1] = q(seed)
    prev = seed
    for i in range(period, len(values)):
        cur = (Decimal(str(values[i])) - prev) * k + prev
        out[i] = q(cur)
        prev = cur
    return out

# ---------------------------------------------------------------- RSI (Wilder)
def rsi_wilder(values, period):
    out = [None] * len(values)
    gains, losses = [], []
    for i in range(1, len(values)):
        ch = Decimal(str(values[i])) - Decimal(str(values[i - 1]))
        gains.append(max(ch, Decimal(0)))
        losses.append(max(-ch, Decimal(0)))
    avg_g = sum(gains[:period]) / period
    avg_l = sum(losses[:period]) / period
    def rsi_from(g, l):
        if l == 0:
            return Decimal(100)
        rs = g / l
        return Decimal(100) - (Decimal(100) / (Decimal(1) + rs))
    out[period] = q(rsi_from(avg_g, avg_l))
    for i in range(period + 1, len(values)):
        # Wilder smoothing: 1/n, NOT 2/(n+1)
        avg_g = (avg_g * (period - 1) + gains[i - 1]) / period
        avg_l = (avg_l * (period - 1) + losses[i - 1]) / period
        out[i] = q(rsi_from(avg_g, avg_l))
    return out

# ---------------------------------------------------------------- ATR (Wilder)
def atr_wilder(highs, lows, closes, period):
    tr = [None]
    for i in range(1, len(closes)):
        h, l, pc = Decimal(str(highs[i])), Decimal(str(lows[i])), Decimal(str(closes[i - 1]))
        tr.append(max(h - l, abs(h - pc), abs(l - pc)))
    out = [None] * len(closes)
    first = sum(tr[1:period + 1]) / period
    out[period] = q(first)
    prev = first
    for i in range(period + 1, len(closes)):
        cur = (prev * (period - 1) + tr[i]) / period
        out[i] = q(cur)
        prev = cur
    return out

print("writing vectors to", OUT)
s10, e10 = sma(CLOSES, 10), ema(CLOSES, 10)
write("sma_10.csv", ["index", "date", "close", "sma_10"],
      [[i, DATES[i].isoformat(), f"{CLOSES[i]:.2f}", "" if s10[i] is None else str(s10[i])]
       for i in range(len(CLOSES))])
write("ema_10.csv", ["index", "date", "close", "ema_10"],
      [[i, DATES[i].isoformat(), f"{CLOSES[i]:.2f}", "" if e10[i] is None else str(e10[i])]
       for i in range(len(CLOSES))])
r14 = rsi_wilder(CLOSES, 14)
write("rsi_14_wilder.csv", ["index", "date", "close", "rsi_14"],
      [[i, DATES[i].isoformat(), f"{CLOSES[i]:.2f}", "" if r14[i] is None else str(r14[i])]
       for i in range(len(CLOSES))])
a14 = atr_wilder(HIGHS, LOWS, CLOSES, 14)
write("atr_14_wilder.csv", ["index", "date", "high", "low", "close", "atr_14"],
      [[i, DATES[i].isoformat(), f"{HIGHS[i]:.2f}", f"{LOWS[i]:.2f}", f"{CLOSES[i]:.2f}",
        "" if a14[i] is None else str(a14[i])] for i in range(len(CLOSES))])

# ---------------------------------------------------------------- weekly rollup
# Monday-start weeks. A partial trailing week IS emitted; a holiday-shortened
# week is a normal week. open = first bar's open, close = last bar's close,
# high/low = extremes, volume = sum.
weeks = {}
for i, d in enumerate(DATES):
    key = d - timedelta(days=d.weekday())
    weeks.setdefault(key, []).append(i)
rows = []
for wk in sorted(weeks):
    idx = weeks[wk]
    rows.append([wk.isoformat(), len(idx),
                 f"{CLOSES[idx[0]]:.2f}",
                 f"{max(HIGHS[i] for i in idx):.2f}",
                 f"{min(LOWS[i] for i in idx):.2f}",
                 f"{CLOSES[idx[-1]]:.2f}",
                 sum(VOLS[i] for i in idx)])
write("weekly_rollup.csv",
      ["week_start", "bar_count", "open", "high", "low", "close", "volume"], rows)

# ---------------------------------------------------------------- contraction ratio
# contraction(n) = ATR(n) over the last n bars / ATR(n) over the n bars before.
# Below 1.0 means volatility is contracting, which is the entry condition.
def simple_atr(h, l, c, start, end):
    tr = []
    for i in range(start, end):
        if i == 0:
            tr.append(Decimal(str(h[i])) - Decimal(str(l[i])))
        else:
            hh, ll, pc = Decimal(str(h[i])), Decimal(str(l[i])), Decimal(str(c[i - 1]))
            tr.append(max(hh - ll, abs(hh - pc), abs(ll - pc)))
    return sum(tr) / len(tr)
N = 5
rows = []
for i in range(len(CLOSES)):
    if i + 1 >= 2 * N:
        recent = simple_atr(HIGHS, LOWS, CLOSES, i + 1 - N, i + 1)
        prior  = simple_atr(HIGHS, LOWS, CLOSES, i + 1 - 2 * N, i + 1 - N)
        rows.append([i, DATES[i].isoformat(), str(q(recent)), str(q(prior)), str(q(recent / prior))])
    else:
        rows.append([i, DATES[i].isoformat(), "", "", ""])
write("contraction_ratio_5.csv",
      ["index", "date", "atr_recent_5", "atr_prior_5", "contraction_ratio"], rows)
print("done")
