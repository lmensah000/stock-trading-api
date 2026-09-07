package com.moneyteam.trading.controller;

import com.moneyteam.common.security.CurrentUserService;
import com.moneyteam.trading.dto.AccountResponseDto;
import com.moneyteam.trading.model.Account;
import com.moneyteam.trading.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Balance and cash movement for the authenticated caller's own account.
 *
 * These routes previously took the account's user id from the path, which let
 * any authenticated caller deposit to or withdraw from another user's account.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final CurrentUserService currentUserService;

    public AccountController(AccountService accountService, CurrentUserService currentUserService) {
        this.accountService = accountService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    public ResponseEntity<AccountResponseDto> getMyAccount() {
        return accountService.getAccount(currentUserService.getCurrentUserId())
                .map(AccountController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/me/deposit")
    public ResponseEntity<AccountResponseDto> deposit(@RequestParam @Positive BigDecimal amount) {
        return ResponseEntity.ok(
                toDto(accountService.deposit(currentUserService.getCurrentUserId(), amount)));
    }

    @PostMapping("/me/withdraw")
    public ResponseEntity<AccountResponseDto> withdraw(@RequestParam @Positive BigDecimal amount) {
        return ResponseEntity.ok(
                toDto(accountService.withdraw(currentUserService.getCurrentUserId(), amount)));
    }

    private static AccountResponseDto toDto(Account account) {
        return new AccountResponseDto(account.getUserId(), account.getCashBalance(), account.getBuyingPower());
    }
}
