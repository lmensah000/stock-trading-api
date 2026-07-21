package com.moneyteam.trading.service.impl;

import com.moneyteam.trading.model.Account;
import com.moneyteam.trading.model.LedgerEntry;
import com.moneyteam.trading.model.enums.LedgerEntryType;
import com.moneyteam.trading.repository.AccountRepository;
import com.moneyteam.trading.repository.LedgerEntryRepository;
import com.moneyteam.trading.service.AccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AccountServiceImpl(AccountRepository accountRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Override
    public Account createAccountForUser(Long userId) {
        return accountRepository.findByUserId(userId).orElseGet(() -> {
            Account account = new Account();
            account.setUserId(userId);
            account.setCashBalance(BigDecimal.ZERO);
            account.setBuyingPower(BigDecimal.ZERO);
            return accountRepository.save(account);
        });
    }

    @Override
    public Optional<Account> getAccount(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    private Account requireAccount(Long userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("No account found for user: " + userId));
    }

    private void recordLedgerEntry(Account account, LedgerEntryType type, BigDecimal amount, Long relatedTradeId) {
        LedgerEntry entry = new LedgerEntry();
        entry.setAccount(account);
        entry.setType(type);
        entry.setAmount(amount);
        entry.setTimestamp(LocalDateTime.now());
        entry.setRelatedTradeId(relatedTradeId);
        ledgerEntryRepository.save(entry);
    }

    @Override
    public Account deposit(Long userId, BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        Account account = requireAccount(userId);
        account.setCashBalance(account.getCashBalance().add(amount));
        account.setBuyingPower(account.getBuyingPower().add(amount));
        Account saved = accountRepository.save(account);
        recordLedgerEntry(saved, LedgerEntryType.DEPOSIT, amount, null);
        return saved;
    }

    @Override
    public Account withdraw(Long userId, BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        Account account = requireAccount(userId);
        if (account.getBuyingPower().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient buying power for withdrawal");
        }
        account.setCashBalance(account.getCashBalance().subtract(amount));
        account.setBuyingPower(account.getBuyingPower().subtract(amount));
        Account saved = accountRepository.save(account);
        recordLedgerEntry(saved, LedgerEntryType.WITHDRAWAL, amount.negate(), null);
        return saved;
    }

    @Override
    public Account debitForBuy(Long userId, BigDecimal amount, Long relatedTradeId) {
        Account account = requireAccount(userId);
        if (account.getBuyingPower().compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient buying power: need " + amount + ", have " + account.getBuyingPower());
        }
        account.setCashBalance(account.getCashBalance().subtract(amount));
        account.setBuyingPower(account.getBuyingPower().subtract(amount));
        Account saved = accountRepository.save(account);
        recordLedgerEntry(saved, LedgerEntryType.TRADE_SETTLEMENT, amount.negate(), relatedTradeId);
        return saved;
    }

    @Override
    public Account creditForSell(Long userId, BigDecimal amount, Long relatedTradeId) {
        Account account = requireAccount(userId);
        account.setCashBalance(account.getCashBalance().add(amount));
        account.setBuyingPower(account.getBuyingPower().add(amount));
        Account saved = accountRepository.save(account);
        recordLedgerEntry(saved, LedgerEntryType.TRADE_SETTLEMENT, amount, relatedTradeId);
        return saved;
    }
}
