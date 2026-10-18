package com.moneyteam.bot.broker;

import java.math.BigDecimal;

/**
 * A position as the broker sees it.
 *
 * Kept distinct from {@code trading.model.Position}, which is ours and is what
 * the live-trading gate reads. Holding both lets them be compared: a drift
 * between our record and the broker's is a reconciliation failure worth
 * noticing, and it cannot be noticed if the two are the same object.
 *
 * @param ticker        instrument
 * @param quantity      shares held; negative for a short
 * @param averageEntry  the broker's average entry price
 * @param marketValue   current value, as the broker marks it
 * @param unrealisedPnL the broker's unrealised figure
 */
public record BrokerPosition(
        String ticker,
        BigDecimal quantity,
        BigDecimal averageEntry,
        BigDecimal marketValue,
        BigDecimal unrealisedPnL) {

    public boolean isLong() {
        return quantity.signum() > 0;
    }
}
