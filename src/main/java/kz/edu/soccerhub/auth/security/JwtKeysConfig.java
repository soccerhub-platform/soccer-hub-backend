package kz.edu.soccerhub.auth.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JwtKeysConfig {

    private static final String DEFAULT_PUBLIC_KEY = "classpath:keys/public.pem";
    private static final String DEFAULT_PRIVATE_KEY = "classpath:keys/private.pem";
    private static final DefaultResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();

    private static String readKeyFile(Resource resource) throws Exception {
        try (var is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Resource readableResource(Resource configured, String fallbackLocation) throws IOException {
        if (configured != null && configured.exists() && configured.isReadable()) {
            return configured;
        }

        Resource fallback = RESOURCE_LOADER.getResource(fallbackLocation);
        if (fallback.exists() && fallback.isReadable()) {
            return fallback;
        }

        String configuredDescription = configured == null ? "<null>" : configured.getDescription();
        throw new IOException("No readable JWT key resource. configured=" + configuredDescription + ", fallback=" + fallbackLocation);
    }

    private static RSAPublicKey loadPublicKey(Resource resource) throws Exception {
        String key = readKeyFile(resource)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(key);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private static RSAPrivateKey loadPrivateKey(Resource resource) throws Exception {
        String key = readKeyFile(resource)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    @Bean
    public RSAPublicKey publicKey(JwtProperties props) throws Exception {
        return loadPublicKey(readableResource(props.getPublicCert(), DEFAULT_PUBLIC_KEY));
    }

    @Bean
    public RSAPrivateKey privateKey(JwtProperties props) throws Exception {
        return loadPrivateKey(readableResource(props.getPrivateCert(), DEFAULT_PRIVATE_KEY));
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAPublicKey publicKey) {
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAPublicKey publicKey, RSAPrivateKey privateKey, JwtProperties props) {
        RSAKey jwk = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(props.getKid())
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }
}
