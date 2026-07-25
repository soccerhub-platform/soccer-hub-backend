package kz.edu.soccerhub.common.dto.trial;

import kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialBookingStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialResult;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TrialBookingDetailsDto(
        UUID id,
        TrialBookingStatus status,
        TrialAttendanceStatus attendanceStatus,
        TrialResult result,

        Student student,
        Lead lead,
        Session session,
        Group group,
        Coach coach,
        Location location,
        Attendance attendance,
        Outcome outcome,
        NextAction nextAction,
        Capabilities capabilities
) {
    @Builder
    public record Student(
            UUID id,
            String fullName,
            LocalDate birthDate,
            Integer age
    ) {}

    @Builder
    public record Lead(
            UUID id,
            String fullName,
            String phone,
            String email
    ) {}

    @Builder
    public record Session(
            UUID id,
            LocalDate date,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            String status
    ) {}

    @Builder
    public record Group(
            UUID id,
            String name
    ) {}

    @Builder
    public record Coach(
            UUID id,
            String fullName
    ) {}

    @Builder
    public record Location(
            UUID id,
            String name
    ) {}

    @Builder
    public record Attendance(
            TrialAttendanceStatus status,
            LocalDateTime markedAt,
            UUID markedBy,
            String comment
    ) {}

    @Builder
    public record Outcome(
            TrialResult result,
            String coachFeedback,
            UUID recommendedGroupId,
            String recommendedGroupName
    ) {}

    @Builder
    public record NextAction(
            String type,
            LocalDateTime dueAt
    ) {}

    @Builder
    public record Capabilities(
            boolean canConfirm,
            boolean canCancel,
            boolean canReschedule,
            boolean canMarkAttendance,
            boolean canRecordResult
    ) {}
}