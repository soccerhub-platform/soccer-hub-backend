package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.media.domain.model.StoredObject;

import java.io.InputStream;

public interface MediaStorage {

    StoredObject store(
            String storageKey,
            InputStream inputStream,
            long contentLength,
            String contentType
    );

    InputStream load(String storageKey);

    void delete(String storageKey);

    boolean exists(String storageKey);
}
