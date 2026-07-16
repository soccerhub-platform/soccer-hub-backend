package kz.edu.soccerhub.coach.application.dto.profile;

import java.util.List;

public record CoachAvailabilityResponse(
        List<String> days,
        String timeFrom,
        String timeTo,
        String timezone,
        boolean configured
) {
}
