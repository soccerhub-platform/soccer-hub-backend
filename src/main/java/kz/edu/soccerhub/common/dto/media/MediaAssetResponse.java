package kz.edu.soccerhub.common.dto.media;

import kz.edu.soccerhub.media.domain.enums.MediaKind;
import kz.edu.soccerhub.media.domain.enums.MediaOwnerType;

import java.time.LocalDateTime;
import java.util.UUID;

public record MediaAssetResponse(
        UUID id,
        MediaOwnerType ownerType,
        UUID ownerId,
        MediaKind kind,
        String fileName,
        String mimeType,
        Long sizeBytes,
        Integer width,
        Integer height,
        String originalUrl,
        String thumbUrl,
        String mediumUrl,
        LocalDateTime createdAt
) {
}
