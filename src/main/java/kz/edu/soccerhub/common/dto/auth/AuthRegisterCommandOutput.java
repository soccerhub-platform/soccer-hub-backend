package kz.edu.soccerhub.common.dto.auth;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AuthRegisterCommandOutput(
        UUID id,
        String email
) {}