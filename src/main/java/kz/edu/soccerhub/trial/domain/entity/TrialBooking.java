package kz.edu.soccerhub.trial.domain.entity;

import jakarta.persistence.*;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialBookingStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialResult;
import lombok.*;

import java.time.LocalDateTime;
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

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

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

    @Column(name = "next_action_type")
    private String nextActionType;

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

    public void confirm() {
        if (status != TrialBookingStatus.SCHEDULED) {
            throw new BadRequestException(
                    "Only scheduled trial can be confirmed"
            );
        }

        status = TrialBookingStatus.CONFIRMED;
        confirmedAt = LocalDateTime.now();
    }

    public void cancel(String reason) {
        if (status != TrialBookingStatus.SCHEDULED
                && status != TrialBookingStatus.CONFIRMED) {
            throw new BadRequestException(
                    "Only scheduled or confirmed trial can be canceled"
            );
        }

        status = TrialBookingStatus.CANCELED;
        cancellationReason = reason;
        canceledAt = LocalDateTime.now();
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
            String coachFeedback
    ) {
        if (status != TrialBookingStatus.COMPLETED) {
            throw new BadRequestException(
                    "Attendance must be marked first"
            );
        }

        this.result = result;
        this.recommendedGroupId = recommendedGroupId;
        this.coachFeedback = coachFeedback;
    }

    public void linkStudent(UUID clientId, UUID studentId) {
        this.clientId = clientId;
        this.studentId = studentId;
    }
}
