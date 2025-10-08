package kz.edu.soccerhub.admin.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CreateAdminInput(
        UUID userId,
        String firstName,
        String lastName,
        String phone,
        @NotNull UUID assignedBranch
) {}