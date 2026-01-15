package com.moneyteam.repository;

import com.moneyteam.model.OptionTradeDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for options trade entity operations.
 */
@Repository
public interface OptionsTradeRepository extends JpaRepository<OptionTradeDetails, Long> {

    Optional<OptionTradeDetails> findByTradeId(Long tradeId);

    List<OptionTradeDetails> findByUnderlyingTicker(String underlyingTicker);

    List<OptionTradeDetails> findByExpirationDateBefore(LocalDate date);

    List<OptionTradeDetails> findByExpirationDateAfter(LocalDate date);

    List<OptionTradeDetails> findByExpirationDateBetween(LocalDate start, LocalDate end);
}
