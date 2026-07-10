package kz.edu.soccerhub.media.application.image;

public record ProcessedImage(
        byte[] content,
        String mimeType,
        String extension,
        int width,
        int height
) {
}
