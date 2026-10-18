package com.moneyteam.bot.broker;

import java.math.BigDecimal;

/**
 * An account snapshot from the broker.
 *
 * {@code paper} is not decoration. Every log line and every status response
 * that mentions an account should be able to say which kind it was, because
 * "did that order go to the paper account or the real one" is the question
 * someone asks after something has already gone wrong, and by then the host
 * configuration may have changed.
 *
 * @param accountId broker's account identifier
 * @param equity    total account value
 * @param cash      settled cash
 * @param buyingPower  what the broker will let us commit
 * @param paper     true when this is a simulated account
 */
public record BrokerAccount(
        String accountId,
        BigDecimal equity,
        BigDecimal cash,
        BigDecimal buyingPower,
        boolean paper) {
}
