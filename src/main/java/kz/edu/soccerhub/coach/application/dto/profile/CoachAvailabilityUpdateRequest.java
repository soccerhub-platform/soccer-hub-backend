package kz.edu.soccerhub.coach.application.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CoachAvailabilityUpdateRequest(
        @NotEmpty(message = "days is required")
        List<String> days,
        @NotBlank(message = "timeFrom is required")
        String timeFrom,
        @NotBlank(message = "timeTo is required")
        String timeTo,
        @NotBlank(message = "timezone is required")
        String timezone
) {
}
