package kz.edu.soccerhub.common.dto.trial;

import kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialBookingStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialNextActionType;
import kz.edu.soccerhub.trial.domain.enums.TrialResult;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TrialBookingListItemDto(
        UUID id,
        UUID leadId,
        UUID clientId,
        UUID studentId,
        UUID trainingSessionId,

        String studentName,
        String leadName,
        String leadPhone,
        String leadEmail,
        LocalDate sessionDate,
        LocalDateTime sessionStartsAt,
        LocalDateTime sessionEndsAt,
        String groupName,
        String coachName,
        String locationName,

        TrialBookingStatus status,
        TrialAttendanceStatus attendanceStatus,
        TrialResult result,
        TrialNextActionType nextActionType,
        LocalDateTime nextActionAt
) {}