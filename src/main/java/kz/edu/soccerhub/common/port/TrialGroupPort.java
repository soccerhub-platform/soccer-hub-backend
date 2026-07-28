package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
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

    /**
     * Retrieves the details of a trial group by its unique identifier.
     *
     * @param groupId the unique identifier of the group
     * @return the details of the trial group
     */
    TrialBookingDetailsDto.Group getDetails(UUID groupId);

    /**
     * Retrieves the details of multiple trial groups by their unique identifiers.
     *
     * @param groupIds the unique identifiers of the groups
     * @return a map where the key is the group ID and the value is the corresponding TrialBookingDetailsDto.Group
     */
    Map<UUID, TrialBookingDetailsDto.Group> getDetails(
            Collection<UUID> groupIds
    );
}
