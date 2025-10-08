package kz.edu.soccerhub.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshInput(@NotBlank String refreshToken) {}