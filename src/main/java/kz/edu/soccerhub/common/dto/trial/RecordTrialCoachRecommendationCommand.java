package kz.edu.soccerhub.common.dto.trial;

import kz.edu.soccerhub.trial.domain.enums.TrialCoachRecommendation;
import lombok.Builder;

import java.util.UUID;

@Builder
public record RecordTrialCoachRecommendationCommand(
        UUID trialId,
        UUID coachId,
        TrialCoachRecommendation recommendation,
        UUID recommendedGroupId,
        String comment
) {
}