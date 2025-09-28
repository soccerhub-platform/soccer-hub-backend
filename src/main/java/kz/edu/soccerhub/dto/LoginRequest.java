package kz.edu.soccerhub.dto;

import lombok.Builder;

@Builder
public record LoginRequest(
        String email,
        String password
) { }
