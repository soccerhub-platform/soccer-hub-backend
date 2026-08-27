package kz.edu.soccerhub.coach.application.dto.session;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CoachSessionTrialStudentItem(
        UUID trialBookingId,
        UUID studentId,
        String name,
        Integer age,
        String attendance,
        String attendanceComment,
        String result,
        String coachFeedback,
        String coachRecommendation,
        UUID coachRecommendedGroupId,
        String coachRecommendationComment,
        LocalDateTime coachRecommendationAt
) {
}