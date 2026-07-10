package kz.edu.soccerhub.media.domain.model;

public record StoredObject(
        String storageKey,
        long sizeBytes,
        String contentType
) {
}
