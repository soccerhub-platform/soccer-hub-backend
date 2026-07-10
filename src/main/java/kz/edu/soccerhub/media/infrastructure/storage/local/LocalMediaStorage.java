package kz.edu.soccerhub.media.infrastructure.storage.local;

import jakarta.annotation.PostConstruct;
import kz.edu.soccerhub.common.port.MediaStorage;
import kz.edu.soccerhub.media.domain.exception.MediaObjectNotFoundException;
import kz.edu.soccerhub.media.domain.exception.MediaStorageException;
import kz.edu.soccerhub.media.domain.model.StoredObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.media.storage",
        name = "type",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalMediaStorage implements MediaStorage {

    private final LocalMediaStorageProperties properties;

    private Path rootPath;

    @PostConstruct
    public void initialize() {
        if (properties.rootPath() == null) {
            throw new IllegalStateException(
                    "Local media storage root path is not configured"
            );
        }

        rootPath = properties.rootPath()
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(rootPath);
            verifyRootDirectory();

            log.info(
                    "Local media storage initialized at: {}",
                    rootPath
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to initialize local media storage",
                    exception
            );
        }
    }

    @Override
    public StoredObject store(
            String storageKey,
            InputStream inputStream,
            long contentLength,
            String contentType
    ) {
        if (inputStream == null) {
            throw new IllegalArgumentException(
                    "Input stream must not be null"
            );
        }

        if (contentLength < 0) {
            throw new IllegalArgumentException(
                    "Content length must not be negative"
            );
        }

        Path targetPath = resolveSafePath(storageKey);
        Path parentPath = targetPath.getParent();
        Path temporaryPath = targetPath.resolveSibling(
                targetPath.getFileName()
                        + ".upload-"
                        + UUID.randomUUID()
                        + ".tmp"
        );

        try {
            Files.createDirectories(parentPath);

            long actualSize = writeToTemporaryFile(
                    inputStream,
                    temporaryPath
            );

            verifyContentLength(
                    contentLength,
                    actualSize
            );

            moveToTarget(
                    temporaryPath,
                    targetPath
            );

            log.debug("Media object stored. key={}, size={}", storageKey, actualSize);

            return new StoredObject(
                    storageKey,
                    actualSize,
                    contentType
            );

        } catch (IOException exception) {
            deleteTemporaryFileQuietly(temporaryPath);
            throw new MediaStorageException(
                    "Failed to store media object: " + storageKey,
                    exception
            );

        } catch (RuntimeException exception) {
            deleteTemporaryFileQuietly(temporaryPath);
            throw exception;
        }
    }

    @Override
    public InputStream load(String storageKey) {
        Path targetPath = resolveSafePath(storageKey);

        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new MediaObjectNotFoundException(storageKey);
        }

        try {
            return new BufferedInputStream(
                    Files.newInputStream(
                            targetPath,
                            StandardOpenOption.READ
                    )
            );

        } catch (IOException exception) {
            throw new MediaStorageException(
                    "Failed to load media object: " + storageKey,
                    exception
            );
        }
    }

    @Override
    public void delete(String storageKey) {
        Path targetPath = resolveSafePath(storageKey);

        try {
            boolean deleted = Files.deleteIfExists(targetPath);

            if (deleted) {
                log.debug(
                        "Media object deleted. key={}",
                        storageKey
                );

                deleteEmptyParentDirectories(
                        targetPath.getParent()
                );
            }

        } catch (IOException exception) {
            throw new MediaStorageException(
                    "Failed to delete media object: " + storageKey,
                    exception
            );
        }
    }

    @Override
    public boolean exists(String storageKey) {
        Path targetPath = resolveSafePath(storageKey);

        return Files.isRegularFile(targetPath);
    }

    private void verifyRootDirectory() {
        if (!Files.isDirectory(rootPath)) {
            throw new IllegalStateException(
                    "Media storage path is not a directory"
            );
        }

        if (!Files.isReadable(rootPath)) {
            throw new IllegalStateException(
                    "Media storage directory is not readable"
            );
        }

        if (!Files.isWritable(rootPath)) {
            throw new IllegalStateException(
                    "Media storage directory is not writable"
            );
        }
    }

    private long writeToTemporaryFile(
            InputStream inputStream,
            Path temporaryPath
    ) throws IOException {
        InputStream source = new BufferedInputStream(inputStream);

        try (
                OutputStream outputStream =
                        new BufferedOutputStream(
                                Files.newOutputStream(
                                        temporaryPath,
                                        StandardOpenOption.CREATE_NEW,
                                        StandardOpenOption.WRITE
                                )
                        )
        ) {
            return source.transferTo(outputStream);
        }
    }

    private void verifyContentLength(
            long contentLength,
            long actualSize
    ) {
        if (
                contentLength > 0
                        && actualSize != contentLength
        ) {
            throw new MediaStorageException(
                    "Stored object size does not match expected size. "
                            + "Expected: " + contentLength
                            + ", actual: " + actualSize
            );
        }
    }

    private Path resolveSafePath(String storageKey) {
        validateStorageKey(storageKey);

        Path resolvedPath = rootPath
                .resolve(storageKey)
                .normalize();

        if (!resolvedPath.startsWith(rootPath)) {
            throw new IllegalArgumentException(
                    "Invalid storage key: " + storageKey
            );
        }

        return resolvedPath;
    }

    private void validateStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Storage key must not be blank");
        }

        if (storageKey.startsWith("/") || Path.of(storageKey).isAbsolute()) {
            throw new IllegalArgumentException("Storage key must be relative");
        }

        if (storageKey.contains("\\")) {
            throw new IllegalArgumentException("Storage key must use '/' as separator");
        }
    }

    private void moveToTarget(
            Path source,
            Path target
    ) throws IOException {
        try {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );

        } catch (IOException exception) {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private void deleteTemporaryFileQuietly(
            Path temporaryPath
    ) {
        try {
            Files.deleteIfExists(temporaryPath);

        } catch (IOException exception) {
            log.warn(
                    "Failed to remove temporary media file: {}",
                    temporaryPath.getFileName(),
                    exception
            );
        }
    }

    private void deleteEmptyParentDirectories(
            Path directory
    ) {
        Path currentDirectory = directory;

        while (currentDirectory != null && !currentDirectory.equals(rootPath)) {

            try (var entries = Files.list(currentDirectory)) {
                if (entries.findAny().isPresent()) {
                    return;
                }
            } catch (IOException exception) {
                log.debug("Could not inspect media directory: {}", currentDirectory.getFileName());
                return;
            }

            try {
                Files.deleteIfExists(currentDirectory);
            } catch (IOException exception) {
                log.debug("Could not delete empty media directory: {}", currentDirectory.getFileName());
                return;
            }

            currentDirectory = currentDirectory.getParent();
        }
    }
}
