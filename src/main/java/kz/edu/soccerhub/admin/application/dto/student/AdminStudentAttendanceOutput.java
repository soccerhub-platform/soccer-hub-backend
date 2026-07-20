package kz.edu.soccerhub.admin.application.dto.student;

import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminStudentAttendanceOutput(
        UUID playerId,
        LocalDate from,
        LocalDate to,
        Summary summary,
        List<Item> items
) {
    public record Summary(
            int sessionsCount,
            int markedCount,
            int presentCount,
            int absentCount,
            int excusedCount,
            int lateCount,
            int unmarkedCount,
            int presentLikeCount,
            int attendanceRate
    ) {
    }

    public record Item(
            UUID sessionId,
            GroupRef group,
            LocalDate sessionDate,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            String sessionStatus,
            String effectiveSessionStatus,
            String attendanceStatus,
            String comment
    ) {
    }

    public record GroupRef(
            UUID id,
            String name,
            MediaAssetResponse avatar
    ) {
    }
}
