package kz.edu.soccerhub.common.dto.coach;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminCoachProfileOutput(
        UUID coachId,
        String firstName,
        String lastName,
        String email,
        String phone,
        boolean active,
        List<GroupItem> groups,
        List<WeeklyScheduleItem> weeklySchedule,
        List<UpcomingSessionItem> upcomingSessions,
        Reports reports,
        List<StatusHistoryItem> statusHistory
) {
    public record GroupItem(
            UUID groupId,
            String groupName,
            UUID branchId
    ) {}

    public record WeeklyScheduleItem(
            UUID scheduleId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            UUID groupId,
            String groupName,
            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record UpcomingSessionItem(
            UUID sessionId,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            UUID groupId,
            String groupName,
            String status,
            boolean reportDone
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
            UUID changedBy
    ) {}
}
