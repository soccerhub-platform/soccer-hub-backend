package kz.edu.soccerhub.coach.application.dto.session;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.trial.domain.enums.TrialCoachRecommendation;

import java.util.UUID;

@lombok.Builder
public record CoachTrialRecommendationInput(
        @NotNull TrialCoachRecommendation recommendation,
        UUID recommendedGroupId,
        String comment,
        @jakarta.validation.constraints.NotNull Long version
) {
    public CoachTrialRecommendationInput(TrialCoachRecommendation recommendation,
        UUID recommendedGroupId,
        String comment) { this(recommendation,recommendedGroupId,comment, null); }
}