package kz.edu.soccerhub.coach.application.dto.session;

import java.time.LocalDate;
import java.util.List;

public record CoachTodaySessionsResponse(
        LocalDate date,
        String timezone,
        List<CoachTodaySessionItem> sessions
) {
}
