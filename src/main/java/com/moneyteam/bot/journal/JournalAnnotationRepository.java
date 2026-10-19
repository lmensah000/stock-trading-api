package com.moneyteam.bot.journal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JournalAnnotationRepository extends JpaRepository<JournalAnnotation, Long> {

    List<JournalAnnotation> findByJournalEntryId(Long journalEntryId);

    /** The same image attached twice is one annotation, not two. */
    Optional<JournalAnnotation> findByJournalEntryIdAndSha256(Long journalEntryId, String sha256);
}
