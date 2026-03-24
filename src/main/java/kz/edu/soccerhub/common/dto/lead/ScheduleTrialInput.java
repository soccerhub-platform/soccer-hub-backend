package kz.edu.soccerhub.common.dto.lead;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ScheduleTrialInput(
        @NotNull(message = "Child id is required")
        UUID childId,

        UUID groupId,

        UUID coachId,

        @NotNull(message = "Slot is required")
        TrialSlotInput slot,

        String comment
) {
}

