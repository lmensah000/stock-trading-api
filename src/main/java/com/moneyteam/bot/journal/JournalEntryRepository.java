package com.moneyteam.bot.journal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    /** Scoped by user, like every other read in this application. */
    Optional<JournalEntry> findByIdAndUserId(Long id, Long userId);

    List<JournalEntry> findByUserIdOrderByFilledAtAsc(Long userId);

    /**
     * Entries at one fidelity tier.
     *
     * Every analysis is scoped this way. Pooling tiers would weight a record
     * holding only daily bars equally with one holding the full chain, and then
     * report a confidence the thin records cannot support.
     */
    List<JournalEntry> findByUserIdAndContextFidelityOrderByFilledAtAsc(
            Long userId, ContextFidelity contextFidelity);

    /** Used to make a re-import a no-op rather than a duplicate. */
    Optional<JournalEntry> findByUserIdAndExternalId(Long userId, String externalId);

    long countByUserIdAndContextFidelity(Long userId, ContextFidelity contextFidelity);
}
