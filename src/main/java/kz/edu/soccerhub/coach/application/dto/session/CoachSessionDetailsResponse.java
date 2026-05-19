package kz.edu.soccerhub.coach.application.dto.session;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CoachSessionDetailsResponse(
        UUID id,
        String groupName,
        LocalDate date,
        String time,
        String status,
        String cancelReason,
        boolean reportDone,
        String attendanceSummary,
        List<CoachSessionStudentItem> students,
        CoachSessionReportView report
) {
}
