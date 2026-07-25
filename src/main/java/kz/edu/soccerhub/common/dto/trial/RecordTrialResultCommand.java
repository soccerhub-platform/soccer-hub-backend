package kz.edu.soccerhub.common.dto.trial;

import kz.edu.soccerhub.trial.domain.enums.TrialResult;
import lombok.Builder;

import java.util.UUID;

@Builder
public record RecordTrialResultCommand(
        UUID trialId,
        TrialResult result,
        UUID recommendedGroupId,
        String coachFeedback,
        UUID adminId
) {}