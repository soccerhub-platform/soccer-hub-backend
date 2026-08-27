package kz.edu.soccerhub.common.dto.trial;

import kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialBookingStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialCoachRecommendation;
import kz.edu.soccerhub.trial.domain.enums.TrialResult;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TrialSessionParticipantDto(
        UUID trialBookingId,
        UUID leadId,
        UUID participantId,
        UUID studentId,
        String fullName,
        LocalDate birthDate,
        Integer age,
        TrialBookingStatus bookingStatus,
        TrialAttendanceStatus attendanceStatus,
        String attendanceComment,
        TrialResult result,
        String coachFeedback,
        TrialCoachRecommendation coachRecommendation,
        UUID coachRecommendedGroupId,
        String coachRecommendationComment,
        LocalDateTime coachRecommendationAt,
        UUID coachRecommendationBy
) {
}