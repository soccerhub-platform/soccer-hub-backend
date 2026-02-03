package kz.edu.soccerhub.admin.application.dto.group;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.organization.domain.model.enums.ScheduleType;

import java.time.LocalDate;
import java.util.UUID;

public record AdminCancelScheduleBatchInput(
        @NotNull UUID coachId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull ScheduleType type
) {
}
