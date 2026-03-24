package kz.edu.soccerhub.common.dto.lead;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record TrialSlotInput(
        @NotNull(message = "Slot date is required")
        LocalDate date,

        @NotNull(message = "Slot start time is required")
        LocalTime startTime
) {
}

