package kz.edu.soccerhub.common.dto.coach;

import java.util.UUID;

public record PlayerAttendanceRateDto(
        UUID playerId,
        int attendanceRate
) {
}
