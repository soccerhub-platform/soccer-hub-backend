package kz.edu.soccerhub.common.dto.group;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Builder
public record DayScheduleSlot(
        @NotNull(message = "Day of week is mandatory")
        DayOfWeek dayOfWeek,
        @NotNull(message = "Start time is mandatory")
        LocalTime startTime,
        @NotNull(message = "End time is mandatory")
        LocalTime endTime
) {}