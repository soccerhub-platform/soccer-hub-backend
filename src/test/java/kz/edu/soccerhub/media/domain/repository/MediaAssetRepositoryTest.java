package kz.edu.soccerhub.media.domain.repository;

import kz.edu.soccerhub.media.domain.enums.MediaKind;
import kz.edu.soccerhub.media.domain.enums.MediaOwnerType;
import kz.edu.soccerhub.media.domain.model.MediaAsset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false"
})
class MediaAssetRepositoryTest {

    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    @Test
    void shouldFindOnlyActiveAssetByOwnerAndKind() {
        UUID ownerId = UUID.randomUUID();
        MediaAsset active = avatar(ownerId, "media/player/active/original.webp");
        MediaAsset deleted = avatar(ownerId, "media/player/deleted/original.webp");
        deleted.markDeleted(
                UUID.randomUUID(),
                LocalDateTime.of(2026, 7, 9, 12, 0)
        );

        mediaAssetRepository.saveAllAndFlush(List.of(
                active,
                deleted
        ));

        var found = mediaAssetRepository.findActiveByOwner(
                MediaOwnerType.PLAYER,
                ownerId,
                MediaKind.AVATAR
        );

        assertTrue(found.isPresent());
        assertEquals(active.getOriginalStorageKey(), found.get().getOriginalStorageKey());
        assertFalse(mediaAssetRepository.findAllActiveByOwner(
                MediaOwnerType.PLAYER,
                ownerId,
                MediaKind.AVATAR
        ).stream().anyMatch(asset -> !asset.isActive()));
    }

    @Test
    void shouldBulkLoadActiveAvatarsForOwners() {
        UUID firstOwnerId = UUID.randomUUID();
        UUID secondOwnerId = UUID.randomUUID();
        UUID deletedOwnerId = UUID.randomUUID();

        MediaAsset deleted = avatar(deletedOwnerId, "media/player/deleted/original.webp");
        deleted.markDeleted(
                UUID.randomUUID(),
                LocalDateTime.of(2026, 7, 9, 12, 0)
        );

        mediaAssetRepository.saveAllAndFlush(List.of(
                avatar(firstOwnerId, "media/player/first/original.webp"),
                avatar(secondOwnerId, "media/player/second/original.webp"),
                deleted
        ));

        List<MediaAsset> assets = mediaAssetRepository.findAllActiveByOwnerTypeAndOwnerIdInAndKind(
                MediaOwnerType.PLAYER,
                List.of(firstOwnerId, secondOwnerId, deletedOwnerId),
                MediaKind.AVATAR
        );

        assertEquals(2, assets.size());
        assertTrue(assets.stream().allMatch(MediaAsset::isActive));
    }

    private MediaAsset avatar(
            UUID ownerId,
            String originalStorageKey
    ) {
        return MediaAsset.builder()
                .ownerType(MediaOwnerType.PLAYER)
                .ownerId(ownerId)
                .kind(MediaKind.AVATAR)
                .fileName("avatar.jpg")
                .mimeType("image/jpeg")
                .sizeBytes(542000L)
                .width(1200)
                .height(1600)
                .originalStorageKey(originalStorageKey)
                .thumbStorageKey(originalStorageKey.replace("original", "thumb"))
                .mediumStorageKey(originalStorageKey.replace("original", "medium"))
                .build();
    }
}
