package kz.edu.soccerhub.common.dto.coach;

import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionAttendanceStatus;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PlayerAttendanceTimelineRecordDto(
        UUID sessionId,
        UUID groupId,
        LocalDate sessionDate,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        TrainingSessionStatus sessionStatus,
        String effectiveSessionStatus,
        TrainingSessionAttendanceStatus attendanceStatus,
        String comment
) {
}
