package com.moneyteam.bot.journal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Reads and writes {@link ContextSnapshot} JSON.
 *
 * <h2>Why this owns its own mapper</h2>
 * A snapshot written with one configuration and read with another is a snapshot
 * that cannot be replayed, and the failure would surface as a disagreement
 * report full of {@code NOT_EVALUABLE} rather than as an obvious error. Pinning
 * the configuration in one place means the writer and the reader cannot drift -
 * which injecting the application's shared mapper would not guarantee, since
 * anything may reconfigure that.
 *
 * Three settings matter:
 * <ul>
 *   <li>{@link JavaTimeModule}, or {@code Instant} fails outright.</li>
 *   <li>Timestamps as ISO-8601 text rather than epoch numbers. A snapshot is
 *       read by people deciding what they saw, and 1767225600 is not.</li>
 *   <li>{@code USE_BIG_DECIMAL_FOR_FLOATS}. Typed {@code BigDecimal} fields
 *       already parse exactly, so this is defensive - but any future untyped
 *       field would otherwise land as a {@code double}, and a price that has
 *       been through binary floating point can no longer be compared exactly.
 *       Gap detection is an exact inequality.</li>
 * </ul>
 */
public final class SnapshotCodec {

    private final ObjectMapper mapper;

    public SnapshotCodec() {
        this.mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                // A snapshot written by a newer version may carry fields this
                // one does not know. Failing on them would make old snapshots
                // unreadable after any addition, which is the opposite of what
                // a write-once archive needs.
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    public String write(ContextSnapshot snapshot) {
        try {
            return mapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialise the context snapshot", e);
        }
    }

    public ContextSnapshot read(String json) {
        try {
            return mapper.readValue(json, ContextSnapshot.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read the context snapshot", e);
        }
    }

    /** The configured mapper, for callers that need the same settings. */
    public ObjectMapper mapper() {
        return mapper;
    }
}
