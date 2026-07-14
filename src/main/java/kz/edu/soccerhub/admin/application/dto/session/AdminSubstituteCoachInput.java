package kz.edu.soccerhub.admin.application.dto.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AdminSubstituteCoachInput(
        @NotNull
        UUID replacedCoachId,
        @NotNull
        UUID substituteCoachId,
        @NotBlank
        String reason
) {
}
