package kz.edu.soccerhub.common.dto.coach;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CoachSessionAdminView(
        UUID sessionId,
        UUID coachId,
        UUID groupId,
        UUID scheduleId,
        String scheduleType,
        UUID locationId,
        LocalDate sessionDate,
        LocalDateTime scheduledStartAt,
        LocalDateTime scheduledEndAt,
        String status,
        boolean reportDone,
        LocalDateTime updatedAt
) {
    public CoachSessionAdminView(
            UUID sessionId,
            UUID coachId,
            UUID groupId,
            UUID scheduleId,
            String scheduleType,
            LocalDate sessionDate,
            LocalDateTime scheduledStartAt,
            LocalDateTime scheduledEndAt,
            String status,
            boolean reportDone,
            LocalDateTime updatedAt
    ) {
        this(sessionId, coachId, groupId, scheduleId, scheduleType, null, sessionDate,
                scheduledStartAt, scheduledEndAt, status, reportDone, updatedAt);
    }
}
