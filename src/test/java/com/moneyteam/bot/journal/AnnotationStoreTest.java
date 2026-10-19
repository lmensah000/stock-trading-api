package com.moneyteam.bot.journal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Storing uploaded chart screenshots.
 *
 * Every test here is about an upload being untrusted input. The declared
 * content type and the filename are both chosen by whoever is uploading, so
 * neither decides anything: the type comes from the file's own bytes and the
 * stored name is generated.
 */
class AnnotationStoreTest {

    private static final long MAX = 5 * 1024 * 1024;

    private static byte[] png(int extraBytes) {
        byte[] bytes = new byte[8 + extraBytes];
        byte[] signature = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(signature, 0, bytes, 0, signature.length);
        return bytes;
    }

    @Test
    @DisplayName("a PNG is stored, and the stored name is generated rather than the client's")
    void storesPngUnderAGeneratedName(@TempDir Path dir) {
        AnnotationStore store = new AnnotationStore(dir, MAX);

        AnnotationStore.Stored stored = store.store(png(100), "my chart.png");

        assertThat(stored.contentType()).isEqualTo("image/png");
        assertThat(stored.byteSize()).isEqualTo(108);
        // Nothing the client sent may appear in a path: a name containing ../
        // is the standard route to writing outside the directory.
        assertThat(stored.storedPath()).doesNotContain("my chart");
        assertThat(Files.exists(dir.resolve(stored.storedPath()))).isTrue();
    }

    @Test
    @DisplayName("a path-traversal filename cannot escape the root")
    void traversalFilenameIsHarmless(@TempDir Path dir) {
        AnnotationStore store = new AnnotationStore(dir, MAX);

        AnnotationStore.Stored stored = store.store(png(10), "../../../etc/passwd.png");

        Path written = dir.resolve(stored.storedPath()).normalize();
        assertThat(written.startsWith(dir.toAbsolutePath().normalize())).isTrue();
        assertThat(stored.storedPath()).doesNotContain("..");
    }

    @Test
    @DisplayName("the type comes from the magic bytes, so renaming a file does not help")
    void typeComesFromTheBytes(@TempDir Path dir) {
        AnnotationStore store = new AnnotationStore(dir, MAX);

        // Claims to be a chart; is actually a shell script.
        byte[] notAnImage = "#!/bin/sh\nrm -rf /\n".getBytes();

        assertThatThrownBy(() -> store.store(notAnImage, "chart.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a PNG, JPEG, GIF or WEBP");
    }

    @Test
    @DisplayName("an oversized file is refused before anything is written")
    void oversizedFileIsRefused(@TempDir Path dir) throws Exception {
        AnnotationStore store = new AnnotationStore(dir, 1024);

        assertThatThrownBy(() -> store.store(png(2048), "big.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("above the");

        // Nothing landed on disk.
        try (var files = Files.walk(dir)) {
            assertThat(files.filter(Files::isRegularFile).count()).isZero();
        }
    }

    @Test
    @DisplayName("an empty upload is refused")
    void emptyUploadIsRefused(@TempDir Path dir) {
        AnnotationStore store = new AnnotationStore(dir, MAX);

        assertThatThrownBy(() -> store.store(new byte[0], "empty.png"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.store(null, "null.png"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("JPEG, GIF and WEBP are recognised; a bare RIFF container is not")
    void recognisesTheAcceptedFormats() {
        assertThat(AnnotationStore.detectImageType(
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0}))
                .isEqualTo("image/jpeg");
        assertThat(AnnotationStore.detectImageType("GIF89a...".getBytes()))
                .isEqualTo("image/gif");
        assertThat(AnnotationStore.detectImageType("RIFF....WEBP".getBytes()))
                .isEqualTo("image/webp");

        // RIFF is a container that can hold audio or video, so the prefix
        // alone is not enough - the WEBP marker at offset 8 must be there too.
        assertThat(AnnotationStore.detectImageType("RIFF....AVI ".getBytes())).isNull();
        assertThat(AnnotationStore.detectImageType("RIFF".getBytes())).isNull();
    }

    @Test
    @DisplayName("the same image stored twice has the same hash, so it can be deduplicated")
    void identicalImagesHashIdentically(@TempDir Path dir) {
        AnnotationStore store = new AnnotationStore(dir, MAX);

        AnnotationStore.Stored first = store.store(png(50), "a.png");
        AnnotationStore.Stored second = store.store(png(50), "b.png");

        // The unique constraint on (entry, sha256) uses this: the same chart
        // attached twice is one annotation.
        assertThat(first.sha256()).isEqualTo(second.sha256());
        assertThat(first.sha256()).hasSize(64);
        // Different generated paths, though - the store does not decide policy.
        assertThat(first.storedPath()).isNotEqualTo(second.storedPath());
    }

    @Test
    @DisplayName("a stored image reads back byte-identical")
    void readsBackUnchanged(@TempDir Path dir) {
        AnnotationStore store = new AnnotationStore(dir, MAX);
        byte[] original = png(200);

        AnnotationStore.Stored stored = store.store(original, "chart.png");

        assertThat(store.read(stored.storedPath())).isEqualTo(original);
    }

    @Test
    @DisplayName("reading a path outside the root is refused")
    void readOutsideRootIsRefused(@TempDir Path dir) {
        AnnotationStore store = new AnnotationStore(dir, MAX);

        assertThatThrownBy(() -> store.read("../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside the annotation root");
    }

    @Test
    @DisplayName("files are filed into dated directories rather than one flat folder")
    void usesDatedDirectories(@TempDir Path dir) {
        AnnotationStore.Stored stored = new AnnotationStore(dir, MAX).store(png(10), "c.png");

        // One folder accumulating thousands of files is slow to list and
        // awkward to prune.
        assertThat(stored.storedPath()).matches("\\d{4}/\\d{2}/[0-9a-f-]+\\.png");
    }
}
