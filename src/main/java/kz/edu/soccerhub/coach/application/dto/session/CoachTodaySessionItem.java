package kz.edu.soccerhub.coach.application.dto.session;

import java.time.LocalDate;
import java.util.UUID;

public record CoachTodaySessionItem(
        UUID id,
        String groupName,
        LocalDate date,
        String time,
        int studentCount,
        String status,
        String cancelReason,
        boolean reportDone,
        String attendanceSummary
) {
}
