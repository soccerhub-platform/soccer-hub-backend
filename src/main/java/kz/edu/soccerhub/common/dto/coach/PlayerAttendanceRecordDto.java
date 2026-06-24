package kz.edu.soccerhub.common.dto.coach;

import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionAttendanceStatus;

import java.time.LocalDate;
import java.util.UUID;

public record PlayerAttendanceRecordDto(
        UUID playerId,
        UUID sessionId,
        LocalDate sessionDate,
        UUID groupId,
        String groupName,
        TrainingSessionAttendanceStatus status
) {
}
