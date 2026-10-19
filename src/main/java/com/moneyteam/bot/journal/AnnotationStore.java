package com.moneyteam.bot.journal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Stores marked-up chart screenshots on disk.
 *
 * <h2>Uploads are untrusted input, and treated as such</h2>
 * <ul>
 *   <li><strong>The type is read from the file's magic bytes</strong>, never
 *       from the declared content type or the extension. Both are
 *       client-controlled, so trusting either means the stored "image" can be
 *       anything.</li>
 *   <li><strong>The stored name is generated.</strong> The client's filename is
 *       recorded for display and never used to build a path - a name containing
 *       {@code ../} is the standard route to writing outside the intended
 *       directory, and a name containing a second extension is the route to
 *       having it served as something executable.</li>
 *   <li><strong>The resolved path is checked to be inside the root</strong>
 *       after normalisation, rather than only before.</li>
 *   <li><strong>Size is capped</strong> before anything is written.</li>
 * </ul>
 *
 * Bytes go to disk and the database keeps a reference. An image per trade
 * carried in every database backup would be a lot of write-once, read-rarely
 * data in the one place that is copied most often.
 *
 * <h2>What this does not do</h2>
 * It does not strip EXIF. A phone screenshot can carry location and device
 * metadata, and the right fix is an image rewrite on ingest - which needs an
 * imaging dependency this project does not have. Flagged rather than silently
 * ignored: for charts captured on a desktop the exposure is small, but it is
 * not nothing and it is not handled.
 */
public class AnnotationStore {

    private static final Logger log = LoggerFactory.getLogger(AnnotationStore.class);

    /** Magic-byte prefixes for the formats a chart screenshot can be. */
    private static final Map<String, byte[]> SIGNATURES = Map.of(
            "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A},
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/gif", new byte[]{'G', 'I', 'F', '8'},
            // WEBP is "RIFF" then four size bytes then "WEBP"; the RIFF prefix
            // is checked here and the WEBP marker at offset 8 below.
            "image/webp", new byte[]{'R', 'I', 'F', 'F'});

    private final Path root;
    private final long maxBytes;

    public AnnotationStore(Path root, long maxBytes) {
        this.root = root.toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    /** What was stored, for the caller to persist. */
    public record Stored(String storedPath, String contentType, long byteSize, String sha256) {
    }

    /**
     * Validates and stores an uploaded image.
     *
     * @param bytes            the whole file; already in memory because the
     *                         size cap is small and streaming to a temporary
     *                         file before validating it would mean writing
     *                         unvalidated bytes to disk first
     * @param originalFilename what the client called it, recorded only
     */
    public Stored store(byte[] bytes, String originalFilename) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("The uploaded file is empty.");
        }
        if (bytes.length > maxBytes) {
            throw new IllegalArgumentException(
                    "The uploaded file is %d bytes, above the %d-byte limit."
                            .formatted(bytes.length, maxBytes));
        }

        String contentType = detectImageType(bytes);
        if (contentType == null) {
            throw new IllegalArgumentException(
                    "The uploaded file is not a PNG, JPEG, GIF or WEBP image. "
                            + "The type is read from the file's own bytes, so renaming it "
                            + "will not help.");
        }

        String sha256 = sha256(bytes);

        // Dated directories so one folder does not accumulate thousands of
        // files, and a generated name so nothing the client sent reaches a path.
        LocalDate today = LocalDate.now();
        String relative = "%04d/%02d/%s%s".formatted(
                today.getYear(), today.getMonthValue(),
                UUID.randomUUID(), extensionFor(contentType));

        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            // Cannot happen with a generated name, and asserted anyway: this is
            // the check that must not be absent if the naming ever changes.
            throw new IllegalStateException("Refusing to write outside the annotation root.");
        }

        try {
            Files.createDirectories(target.getParent());
            // CREATE_NEW, not CREATE: a generated UUID should never collide,
            // and if it somehow does, failing beats overwriting someone's chart.
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store the annotation image", e);
        }

        log.info("Stored annotation {} ({} bytes, {})", relative, bytes.length, contentType);
        return new Stored(relative, contentType, bytes.length, sha256);
    }

    /** Reads a stored image back, checking the path stays inside the root. */
    public byte[] read(String storedPath) {
        Path target = root.resolve(storedPath).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("That path is outside the annotation root.");
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + storedPath, e);
        }
    }

    /**
     * The real type, from the leading bytes, or null when it is not an image
     * this accepts.
     */
    static String detectImageType(byte[] bytes) {
        for (Map.Entry<String, byte[]> candidate : SIGNATURES.entrySet()) {
            if (startsWith(bytes, candidate.getValue())) {
                if ("image/webp".equals(candidate.getKey())) {
                    // RIFF alone is a container; the WEBP marker sits at 8.
                    return bytes.length >= 12
                            && bytes[8] == 'W' && bytes[9] == 'E'
                            && bytes[10] == 'B' && bytes[11] == 'P'
                            ? "image/webp" : null;
                }
                return candidate.getKey();
            }
        }
        return null;
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".bin";
        };
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required and missing", e);
        }
    }
}
