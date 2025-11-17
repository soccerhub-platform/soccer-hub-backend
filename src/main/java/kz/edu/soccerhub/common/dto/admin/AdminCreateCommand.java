package kz.edu.soccerhub.common.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AdminCreateCommand(
        UUID userId,
        String firstName,
        String lastName,
        String phone,
        @NotNull UUID branchId
) {
}
