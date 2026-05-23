package kz.edu.soccerhub.common.dto.coach;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminCoachOverviewOutput(
        Summary summary,
        List<CoachItem> coaches
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
            String status
    ) {}

    public record Reports(
            int overdueCount,
            OffsetDateTime lastReportAt
    ) {}
}
