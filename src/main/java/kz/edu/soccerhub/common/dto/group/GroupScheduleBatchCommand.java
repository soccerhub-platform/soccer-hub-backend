package kz.edu.soccerhub.common.dto.group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.organization.domain.model.enums.ScheduleType;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
public record GroupScheduleBatchCommand(
        @NotNull(message = "Coach identification is mandatory")
        UUID coachId,
        @NotNull(message = "Start date is mandatory")
        LocalDate startDate,
        @NotNull(message = "End date is mandatory")
        LocalDate endDate,
        @NotNull(message = "Schedule type is mandatory")
        ScheduleType type,
        @Valid @NotNull
        List<DayScheduleSlot> slots
) {}