package kz.edu.soccerhub.common.dto.coach;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record CoachWorkingAvailability(
        Set<DayOfWeek> days,
        LocalTime timeFrom,
        LocalTime timeTo,
        String timezone
) {
}
