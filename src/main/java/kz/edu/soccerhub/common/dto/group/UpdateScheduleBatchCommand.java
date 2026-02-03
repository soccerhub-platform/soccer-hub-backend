package kz.edu.soccerhub.common.dto.group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.organization.domain.model.enums.ScheduleType;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
public record UpdateScheduleBatchCommand(
        @NotNull UUID coachId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull ScheduleType type,
        @Valid @NotNull List<DayScheduleSlot> slots
) {
}
