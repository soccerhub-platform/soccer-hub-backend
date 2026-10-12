package kz.edu.soccerhub.coach.application.dto.session;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus;
import lombok.Builder;

@Builder
public record CoachTrialAttendanceInput(
        @NotNull TrialAttendanceStatus status,
        String comment,
        @jakarta.validation.constraints.NotNull Long version
) {
    public CoachTrialAttendanceInput(TrialAttendanceStatus status,
        String comment) { this(status,comment, null); }
}