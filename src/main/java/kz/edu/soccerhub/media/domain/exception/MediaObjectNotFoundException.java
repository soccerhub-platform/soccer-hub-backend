package kz.edu.soccerhub.media.domain.exception;

public class MediaObjectNotFoundException extends MediaStorageException {

    public MediaObjectNotFoundException(String storageKey) {
        super("Media object not found: " + storageKey);
    }
}
