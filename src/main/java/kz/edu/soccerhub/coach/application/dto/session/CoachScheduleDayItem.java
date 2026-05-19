package kz.edu.soccerhub.coach.application.dto.session;

import java.time.LocalDate;
import java.util.List;

public record CoachScheduleDayItem(
        LocalDate date,
        List<CoachScheduleSessionItem> sessions
) {
}
