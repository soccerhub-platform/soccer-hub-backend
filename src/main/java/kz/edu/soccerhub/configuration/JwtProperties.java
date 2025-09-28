package kz.edu.soccerhub.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {

    private String issuer;
    private Duration accessTtl;
    private Duration refreshTtl;
    private String kid;
    private Resource privateCert;
    private Resource publicCert;

}
