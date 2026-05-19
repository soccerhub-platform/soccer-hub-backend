package kz.edu.soccerhub.coach.application.dto.session;

import jakarta.validation.constraints.NotBlank;

public record CoachCancelSessionInput(
        @NotBlank
        String reason
) {
}
