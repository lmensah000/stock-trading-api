package com.moneyteam.trading.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure average-cost lot accounting: no Spring/DB dependency, so every case
 * can be exercised with hand-computed inputs and expected outputs.
 */
public final class PnLCalculator {

    private PnLCalculator() {}

    public static BigDecimal unrealizedPnL(BigDecimal averagePrice, BigDecimal currentPrice, double quantity) {
        return currentPrice.subtract(averagePrice).multiply(BigDecimal.valueOf(quantity));
    }

    public static BigDecimal realizedPnL(BigDecimal averagePrice, BigDecimal exitPrice, double exitQuantity) {
        return exitPrice.subtract(averagePrice)
                .multiply(BigDecimal.valueOf(exitQuantity))
                .setScale(4, RoundingMode.HALF_UP);
    }
}
