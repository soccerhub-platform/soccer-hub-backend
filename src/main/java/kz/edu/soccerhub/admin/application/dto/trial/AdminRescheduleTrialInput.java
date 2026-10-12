package kz.edu.soccerhub.admin.application.dto.trial;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@lombok.Builder
public record AdminRescheduleTrialInput(
        @NotNull UUID trainingSessionId,
        @jakarta.validation.constraints.NotNull Long version
) {
    public AdminRescheduleTrialInput(UUID trainingSessionId) { this(trainingSessionId, null); }
}