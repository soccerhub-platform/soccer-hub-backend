package kz.edu.soccerhub.admin.application.dto.trial;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AdminRescheduleTrialInput(
        @NotNull UUID trainingSessionId
) {
}