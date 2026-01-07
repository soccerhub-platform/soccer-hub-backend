package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.organization.application.dto.CoachBusySlotView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CoachAvailabilityPort {
    List<CoachBusySlotView> getCoachAvailability(
            UUID coachId,
            LocalDate from,
            LocalDate to
    );
}