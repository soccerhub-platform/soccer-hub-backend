package kz.edu.soccerhub.coach.application.dto.session;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.trial.domain.enums.TrialCoachRecommendation;

import java.util.UUID;

public record CoachTrialRecommendationInput(
        @NotNull TrialCoachRecommendation recommendation,
        UUID recommendedGroupId,
        String comment
) {
}