package kz.edu.soccerhub.admin.application.dto.dashboard;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminDashboardSessionDto(
        UUID sessionId,
        UUID groupId,
        String groupName,
        UUID coachId,
        String coachName,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String status,
        String scheduleType
) {
}
