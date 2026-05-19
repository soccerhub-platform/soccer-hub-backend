package kz.edu.soccerhub.coach.application.dto.session;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionAttendanceStatus;

import java.util.UUID;

public record CoachAttendanceStudentInput(
        @NotNull UUID studentId,
        @NotNull TrainingSessionAttendanceStatus attendance
) {
}
