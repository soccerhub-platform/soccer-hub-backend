package kz.edu.soccerhub.coach.application.dto.session;

import java.util.List;

public record CoachScheduleResponse(
        String timezone,
        List<CoachScheduleDayItem> days
) {
}
