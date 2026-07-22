package kz.edu.soccerhub.admin.application.dto.client;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AdminEndClientStudentRelationInput(
        @NotNull LocalDate endedAt
) {
}
