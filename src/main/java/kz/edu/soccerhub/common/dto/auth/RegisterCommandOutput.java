package kz.edu.soccerhub.common.dto.auth;

import lombok.Builder;

import java.util.UUID;

@Builder
public record RegisterCommandOutput(
        UUID id,
        String email
) {}