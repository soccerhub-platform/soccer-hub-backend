package kz.edu.soccerhub.admin.application.dto.trial;

import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;
import kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialBookingStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialResult;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record AdminTrialDetailsOutput(
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

    public static AdminTrialDetailsOutput from(
            TrialBookingDetailsDto details
    ) {
        return AdminTrialDetailsOutput.builder()
                .id(details.id())
                .status(details.status())
                .attendanceStatus(details.attendanceStatus())
                .result(details.result())
                .student(mapStudent(details.student()))
                .lead(mapLead(details.lead()))
                .session(mapSession(details.session()))
                .group(mapGroup(details.group()))
                .coach(mapCoach(details.coach()))
                .location(mapLocation(details.location()))
                .attendance(mapAttendance(details.attendance()))
                .outcome(mapOutcome(details.outcome()))
                .nextAction(mapNextAction(details.nextAction()))
                .capabilities(mapCapabilities(details.capabilities()))
                .build();
    }

    private static Student mapStudent(TrialBookingDetailsDto.Student value) {
        if (value == null) {
            return null;
        }

        return Student.builder()
                .id(value.id())
                .fullName(value.fullName())
                .birthDate(value.birthDate())
                .age(value.age())
                .build();
    }

    private static Lead mapLead(TrialBookingDetailsDto.Lead value) {
        if (value == null) {
            return null;
        }

        return Lead.builder()
                .id(value.id())
                .fullName(value.fullName())
                .phone(value.phone())
                .email(value.email())
                .build();
    }

    private static Session mapSession(TrialBookingDetailsDto.Session value) {
        if (value == null) {
            return null;
        }

        return Session.builder()
                .id(value.id())
                .date(value.date())
                .startsAt(value.startsAt())
                .endsAt(value.endsAt())
                .status(value.status())
                .build();
    }

    private static Group mapGroup(TrialBookingDetailsDto.Group value) {
        if (value == null) {
            return null;
        }

        return Group.builder()
                .id(value.id())
                .name(value.name())
                .build();
    }

    private static Coach mapCoach(TrialBookingDetailsDto.Coach value) {
        if (value == null) {
            return null;
        }

        return Coach.builder()
                .id(value.id())
                .fullName(value.fullName())
                .build();
    }

    private static Location mapLocation(TrialBookingDetailsDto.Location value) {
        if (value == null) {
            return null;
        }

        return Location.builder()
                .id(value.id())
                .name(value.name())
                .build();
    }

    private static Attendance mapAttendance(TrialBookingDetailsDto.Attendance value) {
        if (value == null) {
            return null;
        }

        return Attendance.builder()
                .status(value.status())
                .markedAt(value.markedAt())
                .markedBy(value.markedBy())
                .comment(value.comment())
                .build();
    }

    private static Outcome mapOutcome(TrialBookingDetailsDto.Outcome value) {
        if (value == null) {
            return null;
        }

        return Outcome.builder()
                .result(value.result())
                .coachFeedback(value.coachFeedback())
                .recommendedGroupId(value.recommendedGroupId())
                .recommendedGroupName(value.recommendedGroupName())
                .build();
    }

    private static NextAction mapNextAction(TrialBookingDetailsDto.NextAction value) {
        if (value == null) {
            return null;
        }

        return NextAction.builder()
                .type(value.type())
                .dueAt(value.dueAt())
                .build();
    }

    private static Capabilities mapCapabilities(TrialBookingDetailsDto.Capabilities value) {
        if (value == null) {
            return null;
        }

        return Capabilities.builder()
                .canConfirm(value.canConfirm())
                .canCancel(value.canCancel())
                .canReschedule(value.canReschedule())
                .canMarkAttendance(value.canMarkAttendance())
                .canRecordResult(value.canRecordResult())
                .build();
    }

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