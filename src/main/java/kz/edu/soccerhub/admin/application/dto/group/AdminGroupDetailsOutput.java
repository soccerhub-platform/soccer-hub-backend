package kz.edu.soccerhub.admin.application.dto.group;

import kz.edu.soccerhub.organization.domain.model.enums.GroupAudienceType;
import kz.edu.soccerhub.organization.domain.model.enums.GroupLevel;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminGroupDetailsOutput(
        UUID id,
        String name,
        String description,
        GroupStatus status,
        Integer ageFrom,
        Integer ageTo,
        GroupAudienceType audienceType,
        GroupLevel level,
        Integer capacity,
        BranchRef branch,
        Summary summary,
        GroupHealth health,
        List<AdminGroupHealthOutput.IssueItem> issues,
        NextSession nextSession,
        Capabilities capabilities
) {
    public record BranchRef(
            UUID id,
            String name
    ) {}

    public record Summary(
            int studentsCount,
            int coachesCount,
            int sessionsPerWeek,
            int occupancyPercent
    ) {}

    public record NextSession(
            OffsetDateTime startsAt
    ) {}

    public record Capabilities(
            boolean canEdit,
            boolean canPause,
            boolean canResume,
            boolean canStop,
            boolean canAddStudent
    ) {}
}
