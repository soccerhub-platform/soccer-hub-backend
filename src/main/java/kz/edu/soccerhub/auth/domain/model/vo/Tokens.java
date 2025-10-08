package kz.edu.soccerhub.auth.domain.model.vo;

import java.time.Duration;

public record Tokens(
        String accessToken,
        String refreshToken,
        Duration expiresIn
) {}