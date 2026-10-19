package com.moneyteam.bot.journal;

import com.moneyteam.trading.model.enums.OrderSide;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One recorded fill, with the market as it stood at that moment.
 *
 * The raw material for learning the rules that are traded but never written
 * down. Nothing here asks for a setup to be described: it records what was
 * traded and what was on the screen, and the description is recovered later by
 * comparing behaviour against the rules that <em>are</em> written down.
 *
 * {@code expiry} is a {@link LocalDate}. {@code trading/model/Options} stores
 * its expiration as a {@code String}, which is a known defect, and this does
 * not copy it - expiry arithmetic on strings is how an option looks unexpired
 * forever.
 */
@Entity
@Table(name = "journal_entries")
public class JournalEntry {

    /** Equity or option. */
    public enum Instrument {
        OPTION,
        EQUITY
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "underlying", nullable = false, length = 16)
    private String underlying;

    @Enumerated(EnumType.STRING)
    @Column(name = "instrument", nullable = false, length = 8)
    private Instrument instrument = Instrument.OPTION;

    @Column(name = "option_type", length = 8)
    private String optionType;

    @Column(name = "strike")
    private BigDecimal strike;

    @Column(name = "expiry")
    private LocalDate expiry;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 8)
    private OrderSide side;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "fill_price", nullable = false)
    private BigDecimal fillPrice;

    @Column(name = "filled_at", nullable = false)
    private Instant filledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 24)
    private EntrySource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_fidelity", nullable = false, length = 32)
    private ContextFidelity contextFidelity;

    @Lob
    @Column(name = "snapshot_json")
    private String snapshotJson;

    @Column(name = "closes_entry_id")
    private Long closesEntryId;

    @Column(name = "r_multiple")
    private BigDecimal rMultiple;

    /** The trader's verdict. Deliberately independent of {@link #rMultiple}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "judgement", length = 24)
    private Judgement judgement;

    @Column(name = "judged_at")
    private Instant judgedAt;

    @Column(name = "notes", length = 1024)
    private String notes;

    /**
     * The broker's own fill id, unique per user. Makes re-running an import a
     * no-op rather than a way to double-count a trade, which would corrupt
     * every statistic computed from this table.
     */
    @Column(name = "external_id", length = 128)
    private String externalId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JournalEntry() {
        // for JPA
    }

    public JournalEntry(Long userId,
                        String underlying,
                        OrderSide side,
                        BigDecimal quantity,
                        BigDecimal fillPrice,
                        Instant filledAt,
                        EntrySource source,
                        ContextFidelity contextFidelity,
                        Instant now) {
        this.userId = userId;
        this.underlying = underlying == null ? null : underlying.toUpperCase();
        this.side = side;
        this.quantity = quantity;
        this.fillPrice = fillPrice;
        this.filledAt = filledAt;
        this.source = source;
        this.contextFidelity = contextFidelity;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Marks this fill as an option contract. */
    public JournalEntry asOption(String optionType, BigDecimal strike, LocalDate expiry) {
        this.instrument = Instrument.OPTION;
        this.optionType = optionType;
        this.strike = strike;
        this.expiry = expiry;
        return this;
    }

    public JournalEntry asEquity() {
        this.instrument = Instrument.EQUITY;
        this.optionType = null;
        this.strike = null;
        this.expiry = null;
        return this;
    }

    /**
     * Records the trader's verdict.
     *
     * Takes no outcome and cannot see one: the verdict must be a judgement of
     * the decision, and a method that accepted both would invite one to be
     * derived from the other.
     */
    public JournalEntry judge(Judgement judgement, Instant now) {
        this.judgement = judgement;
        this.judgedAt = now;
        this.updatedAt = now;
        return this;
    }

    /** Links this exit to the entry it closed, and records the outcome in R. */
    public JournalEntry closes(Long entryId, BigDecimal rMultiple, Instant now) {
        this.closesEntryId = entryId;
        this.rMultiple = rMultiple;
        this.updatedAt = now;
        return this;
    }

    public JournalEntry withSnapshot(String snapshotJson, Instant now) {
        this.snapshotJson = snapshotJson;
        this.updatedAt = now;
        return this;
    }

    public JournalEntry withExternalId(String externalId) {
        this.externalId = externalId;
        return this;
    }

    public JournalEntry withNotes(String notes) {
        this.notes = notes;
        return this;
    }

    /** True when option-chain features may be read from this entry. */
    public boolean hasChainContext() {
        return contextFidelity != null && contextFidelity.hasChainContext();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUnderlying() {
        return underlying;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public String getOptionType() {
        return optionType;
    }

    public BigDecimal getStrike() {
        return strike;
    }

    public LocalDate getExpiry() {
        return expiry;
    }

    public OrderSide getSide() {
        return side;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getFillPrice() {
        return fillPrice;
    }

    public Instant getFilledAt() {
        return filledAt;
    }

    public EntrySource getSource() {
        return source;
    }

    public ContextFidelity getContextFidelity() {
        return contextFidelity;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public Long getClosesEntryId() {
        return closesEntryId;
    }

    public BigDecimal getRMultiple() {
        return rMultiple;
    }

    public Judgement getJudgement() {
        return judgement;
    }

    public Instant getJudgedAt() {
        return judgedAt;
    }

    public String getNotes() {
        return notes;
    }

    public String getExternalId() {
        return externalId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Omits the snapshot, which is kilobytes of JSON and useless in a log. */
    @Override
    public String toString() {
        return "JournalEntry{id=" + id
                + ", " + underlying
                + (instrument == Instrument.OPTION
                        ? " " + optionType + " " + strike + " " + expiry : "")
                + ", " + side + " " + quantity + " @ " + fillPrice
                + ", filledAt=" + filledAt
                + ", source=" + source
                + ", fidelity=" + contextFidelity
                + ", judgement=" + judgement
                + ", r=" + rMultiple + "}";
    }
}
