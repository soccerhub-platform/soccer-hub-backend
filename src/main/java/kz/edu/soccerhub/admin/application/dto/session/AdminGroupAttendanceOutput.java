package kz.edu.soccerhub.admin.application.dto.session;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminGroupAttendanceOutput(
        UUID groupId,
        LocalDate from,
        LocalDate to,
        Summary summary,
        List<SessionItem> sessions
) {
    public record Summary(
            int sessionsCount,
            int recordedSessionsCount,
            int totalParticipants,
            int totalMarked,
            int totalPresent,
            int totalAbsent,
            int totalExcused,
            int totalLate,
            int totalUnmarked,
            int totalPresentLike,
            int averageAttendanceRate
    ) {}

    public record SessionItem(
            UUID sessionId,
            LocalDate sessionDate,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            String status,
            String effectiveStatus,
            AdminSessionAttendanceOutput.Summary summary,
            Capabilities capabilities
    ) {}

    public record Capabilities(
            boolean canOpenAttendance,
            boolean canEditAttendance
    ) {}
}
