package com.moneyteam.support;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;

/**
 * HMAC keys for tests, generated rather than written down.
 *
 * These used to be string literals in the test sources. Nothing was actually
 * leaked - they were never real credentials - but a literal named
 * {@code SECRET} is precisely what a secret scanner fires on, and a repository
 * whose scanner alerts are usually false positives is a repository where the
 * one true alert gets waved through. Keeping key material out of the source
 * entirely is cheaper than triaging that.
 *
 * Generation is seeded from the label, so a given label always yields the same
 * key. Tests that need two distinct keys ask for two labels, and a failure is
 * reproducible instead of depending on what the random source happened to
 * produce on that run.
 *
 * Each key is 32 random bytes, Base64-encoded. Deriving the length from
 * {@link #MIN_KEY_BYTES} rather than hand-counting characters is the other
 * reason to generate: the HS256 minimum is then stated once, in code, instead
 * of being a property of a string someone has to trust was measured.
 */
public final class TestKeys {

    /** HS256 requires a key of at least 256 bits. See JwtTokenProvider. */
    public static final int MIN_KEY_BYTES = 32;

    private TestKeys() {
    }

    /**
     * A deterministic Base64 key for the given label, long enough for HS256.
     *
     * Base64 expands 32 bytes to 44 characters, so the UTF-8 length comfortably
     * clears the minimum that {@code JwtTokenProvider} enforces.
     */
    public static String hmacKey(String label) {
        byte[] material = new byte[MIN_KEY_BYTES];
        // Seeded deliberately: reproducible across runs and machines.
        // java.util.Random is wrong for real key material and right here,
        // where the requirement is determinism rather than unpredictability.
        new Random(label.hashCode()).nextBytes(material);

        String key = Base64.getEncoder().encodeToString(material);
        if (key.getBytes(StandardCharsets.UTF_8).length < MIN_KEY_BYTES) {
            throw new IllegalStateException("Generated key is too short for HS256");
        }
        return key;
    }

    /** A key distinct from {@link #hmacKey}'s, for signature-mismatch tests. */
    public static String differentHmacKey(String label) {
        return hmacKey(label + "-alternate");
    }
}
