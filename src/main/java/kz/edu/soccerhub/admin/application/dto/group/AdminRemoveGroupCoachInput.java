package kz.edu.soccerhub.admin.application.dto.group;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

public record AdminRemoveGroupCoachInput(
        UUID replacementCoachId,
        LocalDate effectiveDate,
        @NotBlank String reason
) {
}
