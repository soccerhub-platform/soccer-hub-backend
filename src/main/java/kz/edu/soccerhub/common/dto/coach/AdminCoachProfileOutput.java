package kz.edu.soccerhub.common.dto.coach;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;

public record AdminCoachProfileOutput(
        UUID coachId,
        String firstName,
        String lastName,
        String email,
        LocalDate birthDate,
        String phone,
        String specialization,
        String description,
        MediaAssetResponse avatar,
        boolean active,
        String accountStatus,
        String workStatus,
        LocalDate vacationFrom,
        LocalDate vacationTo,
        String workStatusReason,
        Load load,
        List<GroupItem> groups,
        List<GroupAssignmentHistoryItem> groupAssignmentHistory,
        List<WeeklyScheduleItem> weeklySchedule,
        List<UpcomingSessionItem> upcomingSessions,
        Reports reports,
        List<StatusHistoryItem> statusHistory
) {
    public record GroupItem(
            UUID groupId,
            String groupName,
            MediaAssetResponse avatar,
            UUID branchId,
            UUID groupCoachId,
            String role,
            LocalDate assignedFrom,
            LocalDate assignedTo,
            Integer ageFrom,
            Integer ageTo,
            int studentsCount,
            int activeStudentsCount,
            int weeklySlotsCount,
            NextSessionItem nextSession,
            List<RiskFlagItem> riskFlags
    ) {}

    public record GroupAssignmentHistoryItem(
            UUID groupCoachId,
            UUID groupId,
            String groupName,
            MediaAssetResponse avatar,
            String role,
            LocalDate assignedFrom,
            LocalDate assignedTo,
            String removalReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record NextSessionItem(
            UUID sessionId,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            String status,
            boolean reportDone
    ) {}

    public record RiskFlagItem(
            String code,
            String label,
            String severity
    ) {}

    public record Load(
            int usedSlots,
            int maxSlots,
            String status,
            int completed,
            int planned,
            int used,
            int limit,
            int percentage
    ) {}

    public record WeeklyScheduleItem(
            UUID scheduleId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            String scheduleStatus,
            String scheduleStatusLabel,
            UUID groupId,
            String groupName,
            String coachName,
            LocalDate startDate,
            LocalDate endDate,
            List<ScheduleConflictItem> conflicts
    ) {}

    public record ScheduleConflictItem(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            UUID coachId,
            String coachName,
            UUID conflictingGroupId,
            String conflictingGroupName
    ) {}

    public record UpcomingSessionItem(
            UUID sessionId,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            UUID groupId,
            String groupName,
            String scheduleType,
            LocationItem location,
            String status,
            boolean reportDone
    ) {}

    public record LocationItem(
            UUID id,
            String name
    ) {}

    public record Reports(
            int overdueCount,
            OffsetDateTime lastReportAt,
            List<RecentReportItem> recent
    ) {}

    public record RecentReportItem(
            UUID sessionId,
            LocalDate sessionDate,
            LocalTime startTime,
            UUID groupId,
            String groupName,
            OffsetDateTime reportedAt
    ) {}

    public record StatusHistoryItem(
            String status,
            LocalDateTime changedAt,
            UUID changedBy,
            String eventType,
            String previousAccountStatus,
            String newAccountStatus,
            String previousWorkStatus,
            String newWorkStatus,
            String reason
    ) {}
}
