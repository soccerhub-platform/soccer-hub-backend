package kz.edu.soccerhub.media.application.access;

import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.MediaAccessPort;
import kz.edu.soccerhub.common.port.MediaStorage;
import kz.edu.soccerhub.media.domain.enums.MediaVariant;
import kz.edu.soccerhub.media.domain.model.MediaAsset;
import kz.edu.soccerhub.media.domain.repository.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaAccessService implements MediaAccessPort {

    private final MediaAccessProperties properties;
    private final MediaAccessTokenService tokenService;
    private final MediaAssetRepository mediaAssetRepository;
    private final MediaStorage mediaStorage;

    @Override
    public MediaAssetResponse toResponse(MediaAsset asset) {
        if (asset == null) {
            return null;
        }

        return new MediaAssetResponse(
                asset.getId(),
                asset.getOwnerType(),
                asset.getOwnerId(),
                asset.getKind(),
                asset.getFileName(),
                asset.getMimeType(),
                asset.getSizeBytes(),
                asset.getWidth(),
                asset.getHeight(),
                createContentUrl(asset, MediaVariant.ORIGINAL),
                createContentUrl(asset, MediaVariant.THUMB),
                createContentUrl(asset, MediaVariant.MEDIUM),
                asset.getCreatedAt()
        );
    }

    @Override
    public String createContentUrl(
            MediaAsset asset,
            MediaVariant variant
    ) {
        if (asset == null || storageKey(asset, variant) == null) {
            return null;
        }

        return UriComponentsBuilder.fromPath(normalizeBasePath(properties.basePath()))
                .path("/{assetId}/content")
                .queryParam("variant", variant.apiValue())
                .queryParam("token", tokenService.createToken(asset.getId(), variant))
                .buildAndExpand(asset.getId())
                .toUriString();
    }

    @Transactional(readOnly = true)
    public MediaContent loadContent(
            UUID assetId,
            MediaVariant variant,
            String token
    ) {
        tokenService.validateToken(assetId, variant, token);

        MediaAsset asset = mediaAssetRepository.findById(assetId)
                .filter(MediaAsset::isActive)
                .orElseThrow(() -> new NotFoundException("Media asset not found", assetId));

        String storageKey = storageKey(asset, variant);
        if (storageKey == null) {
            throw new NotFoundException("Media variant not found", variant);
        }

        InputStream inputStream = mediaStorage.load(storageKey);
        return new MediaContent(inputStream, asset.getMimeType(), asset.getFileName());
    }

    private String storageKey(
            MediaAsset asset,
            MediaVariant variant
    ) {
        return switch (variant) {
            case ORIGINAL -> asset.getOriginalStorageKey();
            case THUMB -> asset.getThumbStorageKey();
            case MEDIUM -> asset.getMediumStorageKey();
        };
    }

    private String normalizeBasePath(String basePath) {
        if (basePath == null || basePath.isBlank()) {
            return "/api/media";
        }
        return basePath.startsWith("/") ? basePath : "/" + basePath;
    }
}
