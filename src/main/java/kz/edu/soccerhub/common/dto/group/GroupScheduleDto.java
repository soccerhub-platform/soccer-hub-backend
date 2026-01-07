package kz.edu.soccerhub.common.dto.group;

import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Builder
public record GroupScheduleDto(
        UUID scheduleId,

        UUID groupId,
        UUID coachId,
        UUID branchId,

        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,

        LocalDate startDate,
        LocalDate endDate,

        String scheduleType,
        String status,

        boolean substitution,
        UUID substitutionCoachId
) {
}