package kz.edu.soccerhub.common.dto.trial;

import kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus;
import lombok.Builder;

import java.util.UUID;

@Builder
public record MarkTrialAttendanceCommand(
        UUID trialId,
        TrialAttendanceStatus status,
        String comment,
        UUID adminId
) {}