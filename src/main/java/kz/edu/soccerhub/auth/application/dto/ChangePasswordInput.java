package kz.edu.soccerhub.auth.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ChangePasswordInput(
        @NotNull String newPassword
) {}