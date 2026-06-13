package kz.edu.soccerhub.common.dto.group;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleValidationConflict(
        ScheduleValidationConflictCode code,
        UUID coachId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        LocalTime overlapStart,
        LocalTime overlapEnd,
        UUID conflictingGroupId,
        String conflictingGroupName,
        UUID conflictingScheduleId,
        LocalDate conflictingPeriodStart,
        LocalDate conflictingPeriodEnd,
        String message
) {
}
