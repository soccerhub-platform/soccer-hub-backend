package kz.edu.soccerhub.media.application.storage;

import kz.edu.soccerhub.media.domain.enums.MediaOwnerType;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Component
public class AvatarStorageKeyFactory {

    public AvatarStorageKeys avatarKeys(
            MediaOwnerType ownerType,
            UUID ownerId,
            UUID assetId,
            String extension
    ) {
        String normalizedExtension = extension
                .trim()
                .toLowerCase(Locale.ROOT);
        String basePath = "media/%s/%s/avatar/%s".formatted(
                ownerType.name().toLowerCase(Locale.ROOT),
                ownerId,
                assetId
        );

        return new AvatarStorageKeys(
                basePath + "/original." + normalizedExtension,
                basePath + "/thumb." + normalizedExtension,
                basePath + "/medium." + normalizedExtension
        );
    }
}
