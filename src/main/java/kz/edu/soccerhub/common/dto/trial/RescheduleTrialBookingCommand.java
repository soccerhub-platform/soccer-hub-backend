package kz.edu.soccerhub.common.dto.trial;

import lombok.Builder;

import java.util.UUID;

@Builder
public record RescheduleTrialBookingCommand(
        UUID trialId,
        UUID trainingSessionId,
        UUID adminId
) {
}