package kz.edu.soccerhub.media.application.access;

import java.io.InputStream;

public record MediaContent(
        InputStream inputStream,
        String contentType,
        String fileName
) {
}
