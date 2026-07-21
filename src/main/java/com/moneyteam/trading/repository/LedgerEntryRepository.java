package com.moneyteam.trading.repository;

import com.moneyteam.trading.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByAccountId(Long accountId);
}
