package kz.edu.soccerhub.media.application.access;

import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.media.domain.enums.MediaVariant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaAccessTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final MediaAccessProperties properties;
    private final Clock clock;

    public String createToken(
            UUID assetId,
            MediaVariant variant
    ) {
        long expiresAt = Instant.now(clock).plus(properties.urlTtl()).getEpochSecond();
        String payload = payload(assetId, variant, expiresAt);
        return expiresAt + "." + sign(payload);
    }

    public void validateToken(
            UUID assetId,
            MediaVariant variant,
            String token
    ) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Media access token is required");
        }

        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) {
            throw new BadRequestException("Invalid media access token");
        }

        long expiresAt = parseExpiresAt(parts[0]);
        if (Instant.now(clock).getEpochSecond() > expiresAt) {
            throw new BadRequestException("Media access token expired");
        }

        String expectedSignature = sign(payload(assetId, variant, expiresAt));
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[1].getBytes(StandardCharsets.UTF_8)
        )) {
            throw new BadRequestException("Invalid media access token");
        }
    }

    private long parseExpiresAt(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new BadRequestException("Invalid media access token");
        }
    }

    private String payload(
            UUID assetId,
            MediaVariant variant,
            long expiresAt
    ) {
        return assetId + ":" + variant.name() + ":" + expiresAt;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    properties.tokenSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            ));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign media access token", exception);
        }
    }
}
