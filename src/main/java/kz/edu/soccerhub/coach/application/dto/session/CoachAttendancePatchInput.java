package kz.edu.soccerhub.coach.application.dto.session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CoachAttendancePatchInput(
        @Valid
        @NotNull
        @NotEmpty
        List<CoachAttendanceStudentInput> students
) {
}
