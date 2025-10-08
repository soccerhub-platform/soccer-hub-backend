package kz.edu.soccerhub.auth.application.dto;

import lombok.Builder;

@Builder
public record TokenOutput(
        String accessToken,
        String refreshTokenJti,
        long expiresIn
) {}