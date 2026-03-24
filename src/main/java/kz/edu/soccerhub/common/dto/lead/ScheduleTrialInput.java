package kz.edu.soccerhub.common.dto.lead;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleTrialInput(
        @NotNull(message = "Child id is required")
        UUID childId,

        UUID groupId,

        UUID coachId,

        @NotNull(message = "Trial date is required")
        LocalDate trialDate,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        Integer durationMinutes,

        String comment
) {
    @AssertTrue(message = "Either groupId or coachId is required")
    public boolean hasGroupOrCoach() {
        return groupId != null || coachId != null;
    }
}

