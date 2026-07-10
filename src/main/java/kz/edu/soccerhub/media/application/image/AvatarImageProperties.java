package kz.edu.soccerhub.media.application.image;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.media.avatar")
public record AvatarImageProperties(
        long maxUploadSizeBytes,
        int maxWidth,
        int maxHeight,
        long maxPixels,
        int thumbSize,
        int mediumSize,
        String outputFormat,
        String outputMimeType
) {
}
