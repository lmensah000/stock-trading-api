package com.moneyteam.bot.journal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

/**
 * A marked-up chart attached to a journal entry.
 *
 * Holds a reference, not the bytes. {@code originalFilename} is recorded for
 * display only and never used to build a path - a client-supplied name is
 * untrusted input.
 */
@Entity
@Table(name = "journal_annotations")
public class JournalAnnotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "journal_entry_id", nullable = false)
    private Long journalEntryId;

    @Column(name = "stored_path", nullable = false, length = 512)
    private String storedPath;

    /** Determined from the file's magic bytes, not from what the client sent. */
    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "caption", length = 512)
    private String caption;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected JournalAnnotation() {
        // for JPA
    }

    public JournalAnnotation(Long journalEntryId, AnnotationStore.Stored stored,
                             String originalFilename, String caption, Instant now) {
        this.journalEntryId = journalEntryId;
        this.storedPath = stored.storedPath();
        this.contentType = stored.contentType();
        this.byteSize = stored.byteSize();
        this.sha256 = stored.sha256();
        this.originalFilename = originalFilename;
        this.caption = caption;
        this.uploadedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getJournalEntryId() {
        return journalEntryId;
    }

    public String getStoredPath() {
        return storedPath;
    }

    public String getContentType() {
        return contentType;
    }

    public long getByteSize() {
        return byteSize;
    }

    public String getSha256() {
        return sha256;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getCaption() {
        return caption;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
