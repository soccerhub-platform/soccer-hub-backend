package kz.edu.soccerhub.coach.application.dto.session;

import java.time.LocalDateTime;
import java.util.UUID;

public record CoachScheduleSessionItem(
        UUID id,
        String time,
        String groupName,
        String status,
        String reportStatus,
        LocalDateTime reportDeadline,
        LocalDateTime submittedAt
) {
}
