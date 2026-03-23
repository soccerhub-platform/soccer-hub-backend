package kz.edu.soccerhub.common.dto.lead;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record ScheduleTrialInput(
        @NotNull(message = "Group id is required")
        UUID groupId,

        @NotNull(message = "Coach id is required")
        UUID coachId,

        @NotNull(message = "Trial date is required")
        LocalDateTime trialDate
) {
}

