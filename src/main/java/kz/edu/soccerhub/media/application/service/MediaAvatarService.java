package kz.edu.soccerhub.media.application.service;

import kz.edu.soccerhub.common.port.MediaAvatarPort;
import kz.edu.soccerhub.common.port.MediaStorage;
import kz.edu.soccerhub.media.application.image.AvatarImageProcessor;
import kz.edu.soccerhub.media.application.image.ProcessedAvatar;
import kz.edu.soccerhub.media.application.image.ProcessedImage;
import kz.edu.soccerhub.media.application.storage.AvatarStorageKeyFactory;
import kz.edu.soccerhub.media.application.storage.AvatarStorageKeys;
import kz.edu.soccerhub.media.domain.enums.MediaKind;
import kz.edu.soccerhub.media.domain.enums.MediaOwnerType;
import kz.edu.soccerhub.media.domain.model.MediaAsset;
import kz.edu.soccerhub.media.domain.repository.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaAvatarService implements MediaAvatarPort {

    private static final String FALLBACK_FILE_NAME = "avatar";

    private final AvatarImageProcessor avatarImageProcessor;
    private final AvatarStorageKeyFactory avatarStorageKeyFactory;
    private final MediaStorage mediaStorage;
    private final MediaAssetRepository mediaAssetRepository;

    @Override
    @Transactional
    public MediaAsset uploadAvatar(
            MediaOwnerType ownerType,
            UUID ownerId,
            UUID actorId,
            MultipartFile file
    ) {
        ProcessedAvatar processedAvatar = avatarImageProcessor.process(file);
        UUID assetId = UUID.randomUUID();
        AvatarStorageKeys storageKeys = avatarStorageKeyFactory.avatarKeys(
                ownerType,
                ownerId,
                assetId,
                processedAvatar.original().extension()
        );
        List<String> newlyStoredKeys = new ArrayList<>();

        try {
            store(storageKeys.original(), processedAvatar.original(), newlyStoredKeys);
            store(storageKeys.thumb(), processedAvatar.thumb(), newlyStoredKeys);
            store(storageKeys.medium(), processedAvatar.medium(), newlyStoredKeys);

            MediaAsset oldAvatar = mediaAssetRepository.findActiveByOwner(
                    ownerType,
                    ownerId,
                    MediaKind.AVATAR
            ).orElse(null);

            MediaAsset newAvatar = buildMediaAsset(
                    assetId,
                    ownerType,
                    ownerId,
                    actorId,
                    file,
                    processedAvatar,
                    storageKeys
            );
            List<String> oldStorageKeys = storageKeys(oldAvatar);

            registerTransactionCleanup(
                    assetId,
                    newlyStoredKeys,
                    oldAvatar == null ? null : oldAvatar.getId(),
                    oldStorageKeys
            );

            if (oldAvatar != null) {
                oldAvatar.markDeleted(actorId, LocalDateTime.now());
                mediaAssetRepository.saveAndFlush(oldAvatar);
            }

            MediaAsset saved = mediaAssetRepository.saveAndFlush(newAvatar);

            log.info(
                    "Avatar uploaded. ownerType={}, ownerId={}, newAssetId={}, oldAssetId={}, actorId={}",
                    ownerType,
                    ownerId,
                    saved.getId(),
                    oldAvatar == null ? null : oldAvatar.getId(),
                    actorId
            );

            return saved;

        } catch (RuntimeException exception) {
            deleteStoredKeysQuietly(assetId, newlyStoredKeys);
            throw exception;
        }
    }

    @Override
    @Transactional
    public void deleteAvatar(
            MediaOwnerType ownerType,
            UUID ownerId,
            UUID actorId
    ) {
        MediaAsset oldAvatar = mediaAssetRepository.findActiveByOwner(
                ownerType,
                ownerId,
                MediaKind.AVATAR
        ).orElse(null);

        if (oldAvatar == null) {
            return;
        }

        oldAvatar.markDeleted(actorId, LocalDateTime.now());
        mediaAssetRepository.saveAndFlush(oldAvatar);

        registerAfterCommit(() -> deleteOldStorageKeys(
                oldAvatar.getId(),
                storageKeys(oldAvatar)
        ));

        log.info(
                "Avatar deleted. ownerType={}, ownerId={}, oldAssetId={}, actorId={}",
                ownerType,
                ownerId,
                oldAvatar.getId(),
                actorId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MediaAsset> findActiveAvatar(
            MediaOwnerType ownerType,
            UUID ownerId
    ) {
        return mediaAssetRepository.findActiveByOwner(
                ownerType,
                ownerId,
                MediaKind.AVATAR
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, MediaAsset> findActiveAvatars(
            MediaOwnerType ownerType,
            Collection<UUID> ownerIds
    ) {
        if (ownerIds == null || ownerIds.isEmpty()) {
            return Map.of();
        }

        return mediaAssetRepository.findAllActiveByOwnerTypeAndOwnerIdInAndKind(
                        ownerType,
                        ownerIds,
                        MediaKind.AVATAR
                ).stream()
                .collect(Collectors.toMap(
                        MediaAsset::getOwnerId,
                        Function.identity()
                ));
    }

    private void store(
            String storageKey,
            ProcessedImage image,
            List<String> newlyStoredKeys
    ) {
        mediaStorage.store(
                storageKey,
                new ByteArrayInputStream(image.content()),
                image.content().length,
                image.mimeType()
        );
        newlyStoredKeys.add(storageKey);
    }

    private MediaAsset buildMediaAsset(
            UUID assetId,
            MediaOwnerType ownerType,
            UUID ownerId,
            UUID actorId,
            MultipartFile file,
            ProcessedAvatar processedAvatar,
            AvatarStorageKeys storageKeys
    ) {
        MediaAsset mediaAsset = MediaAsset.builder()
                .id(assetId)
                .ownerType(ownerType)
                .ownerId(ownerId)
                .kind(MediaKind.AVATAR)
                .fileName(safeFileName(file.getOriginalFilename()))
                .mimeType(processedAvatar.originalMimeType())
                .sizeBytes(processedAvatar.originalSizeBytes())
                .width(processedAvatar.originalWidth())
                .height(processedAvatar.originalHeight())
                .originalStorageKey(storageKeys.original())
                .thumbStorageKey(storageKeys.thumb())
                .mediumStorageKey(storageKeys.medium())
                .build();
        mediaAsset.setCreatedBy(actorId.toString());
        return mediaAsset;
    }

    private String safeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return FALLBACK_FILE_NAME;
        }

        String sanitized = originalFilename.replace('\\', '/');
        int separatorIndex = sanitized.lastIndexOf('/');
        if (separatorIndex >= 0) {
            sanitized = sanitized.substring(separatorIndex + 1);
        }

        sanitized = sanitized.trim();
        if (sanitized.isBlank()) {
            return FALLBACK_FILE_NAME;
        }

        return sanitized.length() > 255
                ? sanitized.substring(0, 255)
                : sanitized;
    }

    private void registerTransactionCleanup(
            UUID newAssetId,
            List<String> newlyStoredKeys,
            UUID oldAssetId,
            List<String> oldStorageKeys
    ) {
        Runnable deleteNewFiles = () -> deleteStoredKeysQuietly(newAssetId, newlyStoredKeys);
        Runnable deleteOldFiles = () -> deleteOldStorageKeys(oldAssetId, oldStorageKeys);

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteOldFiles.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteOldFiles.run();
            }

            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteNewFiles.run();
                }
            }
        });
    }

    private void registerAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void deleteOldStorageKeys(
            UUID oldAssetId,
            List<String> oldStorageKeys
    ) {
        for (String storageKey : oldStorageKeys) {
            try {
                mediaStorage.delete(storageKey);

            } catch (RuntimeException exception) {
                log.warn(
                        "Failed to clean old avatar file. assetId={}, storageKey={}",
                        oldAssetId,
                        storageKey,
                        exception
                );
            }
        }
    }

    private void deleteStoredKeysQuietly(
            UUID assetId,
            List<String> storageKeys
    ) {
        for (String storageKey : storageKeys) {
            try {
                mediaStorage.delete(storageKey);

            } catch (RuntimeException exception) {
                log.warn(
                        "Failed to clean avatar file. assetId={}, storageKey={}",
                        assetId,
                        storageKey,
                        exception
                );
            }
        }
    }

    private List<String> storageKeys(MediaAsset asset) {
        if (asset == null) {
            return List.of();
        }

        List<String> keys = new ArrayList<>();
        addIfPresent(keys, asset.getOriginalStorageKey());
        addIfPresent(keys, asset.getThumbStorageKey());
        addIfPresent(keys, asset.getMediumStorageKey());
        return List.copyOf(keys);
    }

    private void addIfPresent(
            List<String> keys,
            String storageKey
    ) {
        if (storageKey != null && !storageKey.isBlank()) {
            keys.add(storageKey);
        }
    }
}
