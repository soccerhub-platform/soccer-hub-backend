package kz.edu.soccerhub.common.dto.coach;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminCoachOverviewOutput(
        Summary summary,
        Page<CoachItem> coaches
) {
    public record Summary(
            int total,
            int active,
            int inactive,
            int withoutGroups,
            int overloaded,
            int withSessionsToday
    ) {}

    public record CoachItem(
            UUID coachId,
            String firstName,
            String lastName,
            String email,
            String phone,
            boolean active,
            String accountStatus,
            String workStatus,
            LocalDate vacationFrom,
            LocalDate vacationTo,
            String workStatusReason,
            String specialization,
            @JsonIgnore LocalDateTime createdAt,
            List<GroupItem> groups,
            int weeklySessionsCount,
            int todaySessionsCount,
            Load load,
            Reports reports
    ) {}

    public record GroupItem(
            UUID groupId,
            String groupName
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

    public record Reports(
            int overdueCount,
            int pendingCount,
            OffsetDateTime lastReportAt
    ) {}
}
