package kz.edu.soccerhub.admin.application.dto.group;

import kz.edu.soccerhub.organization.domain.model.enums.GroupLevel;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminGroupOverviewOutput(
        Summary summary,
        List<GroupItem> groups
) {
    public record Summary(
            int total,
            int active,
            int paused,
            int stopped,
            int withoutCoach,
            int withoutSchedule,
            int overCapacity
    ) {}

    public record GroupItem(
            UUID groupId,
            String name,
            GroupStatus status,
            Integer ageFrom,
            Integer ageTo,
            GroupLevel level,
            Integer capacity,
            int studentsCount,
            int coachesCount,
            boolean scheduleActive,
            OffsetDateTime nextSessionAt,
            GroupHealth health
    ) {}
}
