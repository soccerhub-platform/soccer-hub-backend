package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.media.domain.enums.MediaOwnerType;
import kz.edu.soccerhub.media.domain.model.MediaAsset;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface MediaAvatarPort {

    MediaAsset uploadAvatar(
            MediaOwnerType ownerType,
            UUID ownerId,
            UUID actorId,
            MultipartFile file
    );

    void deleteAvatar(
            MediaOwnerType ownerType,
            UUID ownerId,
            UUID actorId
    );

    Optional<MediaAsset> findActiveAvatar(
            MediaOwnerType ownerType,
            UUID ownerId
    );

    Map<UUID, MediaAsset> findActiveAvatars(
            MediaOwnerType ownerType,
            Collection<UUID> ownerIds
    );
}
