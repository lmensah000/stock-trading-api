package com.moneyteam.trading.controller;

import com.moneyteam.trading.dto.AccountResponseDto;
import com.moneyteam.trading.model.Account;
import com.moneyteam.trading.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<AccountResponseDto> getAccount(@PathVariable Long userId) {
        return accountService.getAccount(userId)
                .map(AccountController::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{userId}/deposit")
    public ResponseEntity<AccountResponseDto> deposit(@PathVariable Long userId, @RequestParam @Positive BigDecimal amount) {
        return ResponseEntity.ok(toDto(accountService.deposit(userId, amount)));
    }

    @PostMapping("/{userId}/withdraw")
    public ResponseEntity<AccountResponseDto> withdraw(@PathVariable Long userId, @RequestParam @Positive BigDecimal amount) {
        return ResponseEntity.ok(toDto(accountService.withdraw(userId, amount)));
    }

    private static AccountResponseDto toDto(Account account) {
        return new AccountResponseDto(account.getUserId(), account.getCashBalance(), account.getBuyingPower());
    }
}
