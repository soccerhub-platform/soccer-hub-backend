package kz.edu.soccerhub.common.dto.lead;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ScheduleTrialInput(
        @NotNull(message = "Participant id is required")
        UUID participantId,

        UUID groupId,

        UUID coachId,

        @NotNull(message = "Slot is required")
        TrialSlotInput slot,

        String comment
) {
}
