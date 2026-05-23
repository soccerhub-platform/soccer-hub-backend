package kz.edu.soccerhub.common.dto.coach;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CoachSessionAdminView(
        UUID sessionId,
        UUID coachId,
        UUID groupId,
        LocalDate sessionDate,
        LocalDateTime scheduledStartAt,
        LocalDateTime scheduledEndAt,
        String status,
        boolean reportDone,
        LocalDateTime updatedAt
) {
}
