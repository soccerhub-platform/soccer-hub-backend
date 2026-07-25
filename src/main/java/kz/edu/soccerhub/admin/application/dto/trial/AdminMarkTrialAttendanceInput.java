package kz.edu.soccerhub.admin.application.dto.trial;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus;
import lombok.Builder;

@Builder
public record AdminMarkTrialAttendanceInput(
        @NotNull TrialAttendanceStatus status,
        String comment
) {
}
