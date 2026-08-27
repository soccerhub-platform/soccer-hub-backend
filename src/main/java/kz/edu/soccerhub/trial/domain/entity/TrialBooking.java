package kz.edu.soccerhub.trial.domain.entity;

import jakarta.persistence.*;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.trial.domain.enums.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "trial_bookings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TrialBooking extends AbstractAuditableEntity {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "participant_id")
    private UUID participantId;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "training_session_id", nullable = false)
    private UUID trainingSessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrialBookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false)
    private TrialAttendanceStatus attendanceStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrialResult result;

    @Column(name = "coach_feedback", columnDefinition = "TEXT")
    private String coachFeedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "coach_recommendation")
    private TrialCoachRecommendation coachRecommendation;

    @Column(name = "coach_recommended_group_id")
    private UUID coachRecommendedGroupId;

    @Column(
            name = "coach_recommendation_comment",
            columnDefinition = "TEXT"
    )
    private String coachRecommendationComment;

    @Column(name = "coach_recommendation_at")
    private LocalDateTime coachRecommendationAt;

    @Column(name = "coach_recommendation_by")
    private UUID coachRecommendationBy;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "attendance_marked_at")
    private LocalDateTime attendanceMarkedAt;

    @Column(name = "attendance_marked_by")
    private UUID attendanceMarkedBy;

    @Column(name = "attendance_comment", columnDefinition = "TEXT")
    private String attendanceComment;

    @Column(name = "recommended_group_id")
    private UUID recommendedGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "next_action_type")
    private TrialNextActionType nextActionType;

    @Column(name = "next_action_at")
    private LocalDateTime nextActionAt;

    public static TrialBooking schedule(
            UUID leadId,
            UUID clientId,
            UUID participantId,
            UUID studentId,
            UUID trainingSessionId) {
        TrialBooking booking = new TrialBooking();

        booking.id = UUID.randomUUID();
        booking.leadId = leadId;
        booking.clientId = clientId;
        booking.participantId = participantId;
        booking.studentId = studentId;
        booking.trainingSessionId = trainingSessionId;
        booking.status = TrialBookingStatus.SCHEDULED;
        booking.attendanceStatus = TrialAttendanceStatus.UNMARKED;
        booking.result = TrialResult.PENDING;

        return booking;
    }

    public static TrialBooking schedule(
            UUID leadId,
            UUID clientId,
            UUID studentId,
            UUID trainingSessionId) {
        return schedule(leadId, clientId, null, studentId, trainingSessionId);
    }

    public void cancel(String reason) {
        if (status != TrialBookingStatus.SCHEDULED) {
            throw new BadRequestException(
                    "Only scheduled trial can be canceled"
            );
        }

        status = TrialBookingStatus.CANCELED;
        cancellationReason = reason;
        canceledAt = LocalDateTime.now();
    }

    public void reschedule(UUID newTrainingSessionId) {
        if (status != TrialBookingStatus.SCHEDULED) {
            throw new BadRequestException(
                    "Only scheduled trial can be rescheduled"
            );
        }

        if (newTrainingSessionId == null) {
            throw new BadRequestException(
                    "New training session is required"
            );
        }

        if (Objects.equals(trainingSessionId, newTrainingSessionId)) {
            throw new BadRequestException(
                    "Trial is already scheduled for this session"
            );
        }

        trainingSessionId = newTrainingSessionId;

        attendanceStatus = TrialAttendanceStatus.UNMARKED;
        attendanceMarkedAt = null;
        attendanceMarkedBy = null;
        attendanceComment = null;

        result = TrialResult.PENDING;
        recommendedGroupId = null;
        coachFeedback = null;
        nextActionType = null;
        nextActionAt = null;
    }

    public void markAttendance(
            TrialAttendanceStatus attendanceStatus,
            UUID adminId,
            String comment
    ) {
        if (status == TrialBookingStatus.CANCELED) {
            throw new BadRequestException(
                    "Canceled trial cannot receive attendance"
            );
        }

        if (status == TrialBookingStatus.COMPLETED) {
            throw new BadRequestException(
                    "Completed trial already has attendance"
            );
        }

        this.attendanceStatus = attendanceStatus;
        this.attendanceMarkedAt = LocalDateTime.now();
        this.attendanceMarkedBy = adminId;
        this.attendanceComment = comment;

        if (attendanceStatus == TrialAttendanceStatus.ATTENDED
                || attendanceStatus == TrialAttendanceStatus.NO_SHOW) {
            this.status = TrialBookingStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
        }
    }

    public void recordResult(
            TrialResult result,
            UUID recommendedGroupId,
            String coachFeedback,
            TrialNextActionType nextActionType,
            LocalDateTime nextActionAt
    ) {
        if (status != TrialBookingStatus.COMPLETED) {
            throw new BadRequestException(
                    "Attendance must be marked first"
            );
        }

        if (attendanceStatus != TrialAttendanceStatus.ATTENDED) {
            throw new BadRequestException(
                    "Result can be recorded only for attended trial"
            );
        }

        if (result == TrialResult.FOLLOW_UP
                && (nextActionType == null || nextActionAt == null)) {
            throw new BadRequestException(
                    "Follow-up action and due date are required"
            );
        }

        this.result = result;
        this.recommendedGroupId = recommendedGroupId;
        this.coachFeedback = coachFeedback;

        this.nextActionType = result == TrialResult.FOLLOW_UP
                ? nextActionType
                : null;

        this.nextActionAt = result == TrialResult.FOLLOW_UP
                ? nextActionAt
                : null;
    }

    public void recordCoachRecommendation(
            TrialCoachRecommendation recommendation,
            UUID recommendedGroupId,
            String comment,
            UUID coachId
    ) {
        if (status != TrialBookingStatus.COMPLETED
                || attendanceStatus
                != TrialAttendanceStatus.ATTENDED) {
            throw new BadRequestException(
                    "Coach recommendation can be recorded only after attended trial"
            );
        }

        if (recommendation == null) {
            throw new BadRequestException(
                    "Coach recommendation is required"
            );
        }

        if (coachId == null) {
            throw new BadRequestException(
                    "Coach id is required"
            );
        }

        if (recommendation
                == TrialCoachRecommendation.RECOMMEND_ANOTHER_GROUP
                && recommendedGroupId == null) {
            throw new BadRequestException(
                    "Recommended group is required"
            );
        }

        this.coachRecommendation = recommendation;

        this.coachRecommendedGroupId =
                recommendation
                        == TrialCoachRecommendation.RECOMMEND_ENROLLMENT
                        || recommendation
                        == TrialCoachRecommendation.RECOMMEND_ANOTHER_GROUP
                        ? recommendedGroupId
                        : null;

        String normalizedComment =
                comment == null ? null : comment.trim();

        this.coachRecommendationComment =
                normalizedComment == null
                        || normalizedComment.isEmpty()
                        ? null
                        : normalizedComment;

        this.coachRecommendationBy = coachId;
        this.coachRecommendationAt = LocalDateTime.now();
    }

    public void linkStudent(UUID clientId, UUID studentId) {
        this.clientId = clientId;
        this.studentId = studentId;
    }
}
