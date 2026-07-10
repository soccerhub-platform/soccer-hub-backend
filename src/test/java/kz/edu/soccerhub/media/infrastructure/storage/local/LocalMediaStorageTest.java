package kz.edu.soccerhub.media.infrastructure.storage.local;

import kz.edu.soccerhub.media.domain.exception.MediaObjectNotFoundException;
import kz.edu.soccerhub.media.domain.exception.MediaStorageException;
import kz.edu.soccerhub.media.domain.model.StoredObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalMediaStorageTest {

    @TempDir
    private Path tempDir;

    @Test
    void initializeShouldCreateConfiguredRootDirectory() {
        Path rootPath = tempDir.resolve("media");

        LocalMediaStorage storage = storage(rootPath);

        assertTrue(Files.isDirectory(rootPath));
        assertFalse(storage.exists("missing.txt"));
    }

    @Test
    void storeShouldSaveObjectAndReturnMetadata() throws IOException {
        LocalMediaStorage storage = storage(tempDir.resolve("media"));
        String storageKey = "player/123/avatar/asset/original.webp";
        byte[] content = "image-content".getBytes(StandardCharsets.UTF_8);

        StoredObject storedObject = storage.store(
                storageKey,
                new ByteArrayInputStream(content),
                content.length,
                "image/webp"
        );

        assertEquals(storageKey, storedObject.storageKey());
        assertEquals(content.length, storedObject.sizeBytes());
        assertEquals("image/webp", storedObject.contentType());
        assertTrue(storage.exists(storageKey));
        assertTrue(Files.isDirectory(tempDir.resolve("media/player/123/avatar/asset")));

        try (InputStream inputStream = storage.load(storageKey)) {
            assertArrayEquals(content, inputStream.readAllBytes());
        }
    }

    @Test
    void storeShouldDeleteTemporaryFileWhenContentLengthDoesNotMatch() throws IOException {
        LocalMediaStorage storage = storage(tempDir.resolve("media"));

        assertThrows(
                MediaStorageException.class,
                () -> storage.store(
                        "objects/file.txt",
                        new ByteArrayInputStream("short".getBytes(StandardCharsets.UTF_8)),
                        100,
                        "text/plain"
                )
        );

        assertFalse(storage.exists("objects/file.txt"));
        try (var paths = Files.list(tempDir.resolve("media/objects"))) {
            assertTrue(paths.noneMatch(path -> path.getFileName().toString().contains(".upload-")));
        }
    }

    @Test
    void loadShouldThrowWhenObjectDoesNotExist() {
        LocalMediaStorage storage = storage(tempDir.resolve("media"));

        assertThrows(
                MediaObjectNotFoundException.class,
                () -> storage.load("missing/file.txt")
        );
    }

    @Test
    void deleteShouldBeIdempotentAndCleanEmptyParents() {
        LocalMediaStorage storage = storage(tempDir.resolve("media"));
        String storageKey = "nested/path/file.txt";

        storage.store(
                storageKey,
                new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)),
                7,
                "text/plain"
        );

        storage.delete(storageKey);
        storage.delete(storageKey);

        assertFalse(storage.exists(storageKey));
        assertTrue(Files.isDirectory(tempDir.resolve("media")));
        assertFalse(Files.exists(tempDir.resolve("media/nested/path")));
    }

    @Test
    void operationsShouldRejectUnsafeStorageKeys() {
        LocalMediaStorage storage = storage(tempDir.resolve("media"));

        assertThrows(IllegalArgumentException.class, () -> storage.exists(null));
        assertThrows(IllegalArgumentException.class, () -> storage.exists(" "));
        assertThrows(IllegalArgumentException.class, () -> storage.exists("/absolute/file.txt"));
        assertThrows(IllegalArgumentException.class, () -> storage.exists("folder\\file.txt"));
        assertThrows(IllegalArgumentException.class, () -> storage.exists("../../etc/passwd"));
    }

    private LocalMediaStorage storage(Path rootPath) {
        LocalMediaStorage storage = new LocalMediaStorage(
                new LocalMediaStorageProperties(rootPath)
        );

        storage.initialize();

        return storage;
    }
}
