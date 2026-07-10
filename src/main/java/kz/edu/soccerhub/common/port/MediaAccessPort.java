package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;
import kz.edu.soccerhub.media.domain.enums.MediaVariant;
import kz.edu.soccerhub.media.domain.model.MediaAsset;

public interface MediaAccessPort {

    MediaAssetResponse toResponse(MediaAsset asset);

    String createContentUrl(
            MediaAsset asset,
            MediaVariant variant
    );
}
