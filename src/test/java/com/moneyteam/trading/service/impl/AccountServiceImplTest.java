package com.moneyteam.trading.service.impl;

import com.moneyteam.trading.model.Account;
import com.moneyteam.trading.model.LedgerEntry;
import com.moneyteam.trading.model.enums.LedgerEntryType;
import com.moneyteam.trading.repository.AccountRepository;
import com.moneyteam.trading.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cash movement and the buying-power gate. Before this existed a BUY could be
 * recorded against an account with no funds at all.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    private static final long USER = 3L;

    @Mock private AccountRepository accountRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;
    @InjectMocks private AccountServiceImpl accountService;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setUserId(USER);
        account.setCashBalance(new BigDecimal("1000.00"));
        account.setBuyingPower(new BigDecimal("1000.00"));
    }

    private void accountExists() {
        when(accountRepository.findByUserId(USER)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("a BUY within buying power debits both cash and buying power")
    void debitForBuyWithinMeans() {
        accountExists();

        accountService.debitForBuy(USER, new BigDecimal("400.00"), 99L);

        assertThat(account.getCashBalance()).isEqualByComparingTo("600.00");
        assertThat(account.getBuyingPower()).isEqualByComparingTo("600.00");
    }

    @Test
    @DisplayName("a BUY beyond buying power is rejected and moves no money")
    void debitForBuyBeyondMeans() {
        when(accountRepository.findByUserId(USER)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.debitForBuy(USER, new BigDecimal("1000.01"), 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient buying power");

        assertThat(account.getCashBalance()).isEqualByComparingTo("1000.00");
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("spending exactly the available buying power is allowed")
    void debitExactlyAvailable() {
        accountExists();

        accountService.debitForBuy(USER, new BigDecimal("1000.00"), 99L);

        assertThat(account.getBuyingPower()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("a BUY writes a negative TRADE_SETTLEMENT entry linked to the trade")
    void buyWritesLedgerEntry() {
        accountExists();

        accountService.debitForBuy(USER, new BigDecimal("400.00"), 99L);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(captor.capture());
        LedgerEntry entry = captor.getValue();
        assertThat(entry.getType()).isEqualTo(LedgerEntryType.TRADE_SETTLEMENT);
        assertThat(entry.getAmount()).isEqualByComparingTo("-400.00");
        assertThat(entry.getRelatedTradeId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("a SELL credits proceeds and writes a positive settlement entry")
    void creditForSell() {
        accountExists();

        accountService.creditForSell(USER, new BigDecimal("250.00"), 100L);

        assertThat(account.getCashBalance()).isEqualByComparingTo("1250.00");
        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("a non-positive deposit is rejected")
    void depositMustBePositive() {
        assertThatThrownBy(() -> accountService.deposit(USER, new BigDecimal("0")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> accountService.deposit(USER, new BigDecimal("-5")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a withdrawal beyond buying power is rejected")
    void withdrawBeyondMeans() {
        when(accountRepository.findByUserId(USER)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.withdraw(USER, new BigDecimal("1000.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient buying power");
    }

    @Test
    @DisplayName("acting on an account that does not exist fails rather than creating one")
    void missingAccountFails() {
        when(accountRepository.findByUserId(USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.debitForBuy(USER, new BigDecimal("1.00"), 1L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("creating an account for a new user starts it at zero")
    void newAccountStartsEmpty() {
        when(accountRepository.findByUserId(USER)).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        Account created = accountService.createAccountForUser(USER);

        assertThat(created.getCashBalance()).isEqualByComparingTo("0");
        assertThat(created.getBuyingPower()).isEqualByComparingTo("0");
    }
}
