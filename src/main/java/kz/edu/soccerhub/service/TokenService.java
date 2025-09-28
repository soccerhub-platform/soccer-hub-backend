package kz.edu.soccerhub.service;

import jakarta.annotation.Nullable;
import kz.edu.soccerhub.configuration.JwtProperties;
import kz.edu.soccerhub.model.AppUser;
import kz.edu.soccerhub.model.RefreshToken;
import kz.edu.soccerhub.repository.RefreshTokenRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtEncoder encoder;
    private final RefreshTokenRepo refreshRepo;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom RNG = new SecureRandom();

    public record Tokens(String accessToken, String refreshToken, Duration expiresIn) {}
    public record RefreshResult(AppUser user, String newRefresh) {}

    private static String randomBase64Url(int numBytes) {
        byte[] bytes = new byte[numBytes];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Генерация только access-токена */
    public String issueAccessToken(AppUser user, List<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plus(jwtProperties.getAccessTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiresAt(exp)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .claim("scope", "api")
                .id(UUID.randomUUID().toString())
                .build();

        return encoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(() -> "RS256").build(), claims)
        ).getTokenValue();
    }

    /** Генерация access+refresh (используется при login/регистрации) */
    public Tokens issueTokens(AppUser user, List<String> roles, @Nullable String userAgent) {
        String access = issueAccessToken(user, roles);

        Instant now = Instant.now();
        UUID jti = UUID.randomUUID();
        Instant exp = now.plus(jwtProperties.getRefreshTtl());

        String secret = randomBase64Url(24);
        String rawRefresh = jti + "." + secret;
        String hash = passwordEncoder.encode(secret);

        RefreshToken rt = new RefreshToken();
        rt.setId(UUID.randomUUID());
        rt.setUser(user);
        rt.setJti(jti);
        rt.setTokenHash(hash);
        rt.setIssuedAt(LocalDateTime.ofInstant(now, ZoneOffset.UTC));
        rt.setExpiresAt(LocalDateTime.ofInstant(exp, ZoneOffset.UTC));
        rt.setRevoked(false);
        rt.setUserAgent(userAgent);
        refreshRepo.save(rt);

        return new Tokens(access, rawRefresh, jwtProperties.getAccessTtl());
    }

    /** Проверка + ротация refresh */
    public Optional<RefreshResult> validateAndRotateRefresh(String rawToken, @Nullable String userAgent) {
        if (rawToken == null) return Optional.empty();
        String[] parts = rawToken.split("\\.", 2);
        if (parts.length != 2) return Optional.empty();

        UUID jti;
        try { jti = UUID.fromString(parts[0]); } catch (Exception e) { return Optional.empty(); }
        String secret = parts[1];

        var opt = refreshRepo.findByJti(jti);
        if (opt.isEmpty()) return Optional.empty();

        RefreshToken stored = opt.get();
        if (stored.isRevoked() || LocalDateTime.now(ZoneOffset.UTC).isAfter(stored.getExpiresAt())) {
            return Optional.empty();
        }
        if (!passwordEncoder.matches(secret, stored.getTokenHash())) {
            return Optional.empty();
        }

        // ротация
        stored.setRevoked(true);

        UUID newJti = UUID.randomUUID();
        String newSecret = randomBase64Url(24);
        String newRaw = newJti + "." + newSecret;
        String newHash = passwordEncoder.encode(newSecret);

        RefreshToken next = new RefreshToken();
        next.setId(UUID.randomUUID());
        next.setUser(stored.getUser());
        next.setJti(newJti);
        next.setTokenHash(newHash);
        next.setIssuedAt(LocalDateTime.now(ZoneOffset.UTC));
        next.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(jwtProperties.getRefreshTtl()));
        next.setRevoked(false);
        next.setUserAgent(userAgent);

        stored.setReplacedByJty(newJti);

        refreshRepo.save(stored);
        refreshRepo.save(next);

        return Optional.of(new RefreshResult(stored.getUser(), newRaw));
    }
}