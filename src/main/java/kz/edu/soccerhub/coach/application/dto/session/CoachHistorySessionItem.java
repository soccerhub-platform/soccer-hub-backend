package kz.edu.soccerhub.coach.application.dto.session;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CoachHistorySessionItem(
        UUID id,
        LocalDate date,
        String groupName,
        String status,
        String attendanceSummary,
        boolean reportDone,
        String reportStatus,
        LocalDateTime reportDeadline,
        LocalDateTime submittedAt
) {
}
