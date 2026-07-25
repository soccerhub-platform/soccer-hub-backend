package kz.edu.soccerhub.admin.application.dto.trial;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CreateTrialBookingInput(
        UUID leadId,

        UUID clientId,

        UUID participantId,

        UUID studentId,

        @NotNull
        UUID trainingSessionId
) {
}
