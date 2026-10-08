package kz.edu.soccerhub.auth.application.dto;

import lombok.Builder;
import kz.edu.soccerhub.common.domain.enums.Role;

import java.util.Set;

@Builder
public record TokenOutput(
        String accessToken,
        String refreshTokenJti,
        long expiresIn,
        boolean passwordChangeRequired,
        Set<Role> roles
) {}
