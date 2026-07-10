package kz.edu.soccerhub.media.infrastructure.storage.local;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.media.storage.local")
public record LocalMediaStorageProperties(
        Path rootPath
) {
}
