package kz.edu.soccerhub.admin.application.dto.trial;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.trial.domain.enums.TrialResult;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AdminRecordTrialResultInput(
        @NotNull TrialResult result,
        UUID recommendedGroupId,
        String coachFeedback
) {
}
