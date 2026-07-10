package kz.edu.soccerhub.media.application.image;

public record ProcessedAvatar(
        String originalMimeType,
        long originalSizeBytes,
        int originalWidth,
        int originalHeight,
        ProcessedImage original,
        ProcessedImage thumb,
        ProcessedImage medium
) {
}
