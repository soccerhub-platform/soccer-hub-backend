package kz.edu.soccerhub.auth.application.service;

import jakarta.annotation.Nullable;
import kz.edu.soccerhub.auth.domain.model.AppUserEntity;
import kz.edu.soccerhub.auth.domain.model.RefreshTokenEntity;
import kz.edu.soccerhub.auth.domain.model.vo.RotatedRefreshToken;
import kz.edu.soccerhub.auth.domain.model.vo.Tokens;
import kz.edu.soccerhub.auth.domain.repository.RefreshTokenRepo;
import kz.edu.soccerhub.auth.security.JwtProperties;
import kz.edu.soccerhub.common.domain.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepo refreshRepo;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom RNG = new SecureRandom();

    public String issueAccessToken(AppUserEntity user, Set<Role> roles) {
        JwtClaimsSet claims = buildAccessClaims(user, roles);
        return jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(() -> "RS256").build(), claims)
        ).getTokenValue();
    }

    public Tokens issueTokens(AppUserEntity user, Set<Role> roles, @Nullable String userAgent) {
        String accessToken = issueAccessToken(user, roles);
        String refreshToken = createAndStoreRefreshToken(user, userAgent);
        return new Tokens(accessToken, refreshToken, jwtProperties.getAccessTtl());
    }

    public Optional<RotatedRefreshToken> validateAndRotateRefresh(String rawToken, @Nullable String userAgent) {
        if (rawToken == null || !rawToken.contains(".")) {
            return Optional.empty();
        }

        String[] parts = rawToken.split("\\.", 2);
        UUID jti = tryParseUUID(parts[0]);
        String secret = parts[1];

        if (jti == null || secret == null) {
            return Optional.empty();
        }

        return refreshRepo.findByJti(jti)
                .filter(token -> isValidRefreshToken(token, secret))
                .map(oldToken -> rotateRefreshToken(oldToken, userAgent));
    }

    @Transactional
    public void revokeAllRefreshTokens(UUID userId) {
        refreshRepo.revokeAllByUserId(userId);
    }

    // ========== Private Helpers ==========

    private JwtClaimsSet buildAccessClaims(AppUserEntity user, Set<Role> roles) {
        Instant now = Instant.now();
        return JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.getAccessTtl()))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .claim("scope", "api")
                .id(UUID.randomUUID().toString())
                .build();
    }

    private String createAndStoreRefreshToken(AppUserEntity user, @Nullable String userAgent) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getRefreshTtl());

        UUID jti = UUID.randomUUID();
        String secret = generateRandomSecret();
        String rawToken = jti + "." + secret;
        String hash = passwordEncoder.encode(secret);

        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .jti(jti)
                .tokenHash(hash)
                .issuedAt(toUtc(now))
                .expiresAt(toUtc(expiresAt))
                .revoked(false)
                .userAgent(userAgent)
                .build();

        refreshRepo.save(refreshToken);
        return rawToken;
    }

    private RotatedRefreshToken rotateRefreshToken(RefreshTokenEntity oldToken, @Nullable String userAgent) {
        oldToken.setRevoked(true);

        // Create new token
        UUID newJti = UUID.randomUUID();
        String newSecret = generateRandomSecret();
        String newRaw = newJti + "." + newSecret;
        String newHash = passwordEncoder.encode(newSecret);

        RefreshTokenEntity newToken = RefreshTokenEntity.builder()
                .id(UUID.randomUUID())
                .user(oldToken.getUser())
                .jti(newJti)
                .tokenHash(newHash)
                .issuedAt(nowUtc())
                .expiresAt(nowUtc().plus(jwtProperties.getRefreshTtl()))
                .revoked(false)
                .userAgent(userAgent)
                .build();

        oldToken.setReplacedByJty(newJti);

        refreshRepo.save(oldToken);
        refreshRepo.save(newToken);

        return new RotatedRefreshToken(oldToken.getUser(), newRaw);
    }

    private boolean isValidRefreshToken(RefreshTokenEntity token, String secret) {
        return !token.isRevoked()
                && nowUtc().isBefore(token.getExpiresAt())
                && passwordEncoder.matches(secret, token.getTokenHash());
    }

    private UUID tryParseUUID(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String generateRandomSecret() {
        byte[] bytes = new byte[24];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private LocalDateTime toUtc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}