package kz.edu.soccerhub.common.dto.coach;

import java.util.UUID;

public record PlayerAttendanceSummaryDto(
        UUID playerId,
        int attendanceRate,
        int presentCount,
        int absentCount,
        int lateCount,
        int excusedCount,
        int missedLast30Days
) {
}
