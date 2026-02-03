package kz.edu.soccerhub.common.dto.group;

import kz.edu.soccerhub.organization.domain.model.enums.ScheduleType;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record CancelScheduleBatchCommand(
        UUID coachId,
        LocalDate startDate,
        LocalDate endDate,
        ScheduleType type
) {}