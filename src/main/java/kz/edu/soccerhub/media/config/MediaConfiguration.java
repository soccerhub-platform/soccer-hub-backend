package kz.edu.soccerhub.media.config;

import kz.edu.soccerhub.media.application.access.MediaAccessProperties;
import kz.edu.soccerhub.media.application.image.AvatarImageProperties;
import kz.edu.soccerhub.media.infrastructure.storage.local.LocalMediaStorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(
        {
                LocalMediaStorageProperties.class,
                AvatarImageProperties.class,
                MediaAccessProperties.class
        }
)
public class MediaConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
