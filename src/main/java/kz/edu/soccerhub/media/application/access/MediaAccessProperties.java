package kz.edu.soccerhub.media.application.access;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.media.access")
public record MediaAccessProperties(
        String basePath,
        String tokenSecret,
        Duration urlTtl
) {
    public MediaAccessProperties {
        if (basePath == null || basePath.isBlank()) {
            basePath = "/api/media";
        }
        if (urlTtl == null) {
            urlTtl = Duration.ofMinutes(15);
        }
    }
}
