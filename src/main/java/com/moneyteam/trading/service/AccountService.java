package com.moneyteam.trading.service;

import com.moneyteam.trading.model.Account;

import java.math.BigDecimal;
import java.util.Optional;

public interface AccountService {

    Account createAccountForUser(Long userId);

    Optional<Account> getAccount(Long userId);

    Account deposit(Long userId, BigDecimal amount);

    Account withdraw(Long userId, BigDecimal amount);

    /** Debits cash/buying power for a BUY trade settlement; throws if buying power is insufficient. */
    Account debitForBuy(Long userId, BigDecimal amount, Long relatedTradeId);

    /** Credits cash/buying power with SELL proceeds. */
    Account creditForSell(Long userId, BigDecimal amount, Long relatedTradeId);
}
