package kz.edu.soccerhub.organization.application.dto;

import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Builder
public record CoachBusySlotView(
        UUID scheduleId,
        UUID groupId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate startDate,
        LocalDate endDate
) {
}