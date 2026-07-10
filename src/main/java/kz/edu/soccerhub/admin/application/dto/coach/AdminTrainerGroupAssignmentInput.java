package kz.edu.soccerhub.admin.application.dto.coach;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;

import java.time.LocalDate;
import java.util.UUID;

public record AdminTrainerGroupAssignmentInput(
        @NotNull UUID groupId,
        CoachRole role,
        LocalDate assignedFrom,
        LocalDate assignedTo
) {
}
