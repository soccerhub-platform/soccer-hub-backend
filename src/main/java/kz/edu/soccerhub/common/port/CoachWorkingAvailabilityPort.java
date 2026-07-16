package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.coach.CoachWorkingAvailability;

import java.util.Optional;
import java.util.UUID;

public interface CoachWorkingAvailabilityPort {
    Optional<CoachWorkingAvailability> findWorkingAvailability(UUID coachId);
}
