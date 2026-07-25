package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;

import java.time.LocalDate;
import java.util.UUID;

public interface TrialGroupPort {

    /**
     * Validates that the group with the given ID has available capacity for trial bookings.
     * @param groupId the unique identifier of the group to validate
     */
    void validateAvailableCapacity(
            UUID groupId,
            UUID studentId,
            LocalDate asOfDate
    );

    TrialBookingDetailsDto.Group getDetails(UUID groupId);
}
