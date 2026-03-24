package kz.edu.soccerhub.common.dto.lead;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailableSlotOutput(
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
) {
}

