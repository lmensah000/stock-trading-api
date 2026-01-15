package com.moneyteam.repository;

import com.moneyteam.model.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Watchlist entity operations.
 */
@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    List<Watchlist> findByUserWatchListId(Long userId);

    Optional<Watchlist> findByIdAndUserWatchListId(Long id, Long userId);

    void deleteByIdAndUserWatchListId(Long id, Long userId);

    boolean existsByIdAndUserWatchListId(Long id, Long userId);
}
