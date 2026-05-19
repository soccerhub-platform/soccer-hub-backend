package kz.edu.soccerhub.coach.application.dto.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CoachReportSaveInput(
        @NotBlank
        @Size(max = 255)
        String topic,
        String coachComment,
        String incidents,
        String homework
) {
}
