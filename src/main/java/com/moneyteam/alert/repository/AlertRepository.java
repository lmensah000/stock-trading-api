package com.moneyteam.alert.repository;

import com.moneyteam.alert.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    // Every finder is user-scoped. An unscoped variant would return one user's
    // alerts to another, the same hole the trading routes already closed.
    List<Alert> findByUserIdOrderByDetectedAtDesc(Long userId);

    List<Alert> findByUserIdAndAcknowledgedAtIsNullOrderByDetectedAtDesc(Long userId);

    Optional<Alert> findByIdAndUserId(Long id, Long userId);

    /** Backs the cooldown: has this exact condition already alerted recently? */
    boolean existsByUserIdAndDedupeKeyAndDetectedAtAfter(Long userId, String dedupeKey, Instant since);
}
