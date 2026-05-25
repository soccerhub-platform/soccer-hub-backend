package kz.edu.soccerhub.admin.application.dto.group;

import java.time.OffsetDateTime;

public record AdminGroupScheduleRiskOutput(
        boolean hasConflicts,
        int conflictsCount,
        int emptyDaysCount,
        OffsetDateTime nextSessionAt
) {
}
