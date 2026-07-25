package kz.edu.soccerhub.common.dto.trial;

import lombok.Builder;

import java.util.UUID;

@Builder
public record CreateTrialBookingCommand(
        UUID leadId,
        UUID clientId,
        UUID participantId,
        UUID studentId,
        UUID trainingSessionId,
        UUID adminId
) {
}
