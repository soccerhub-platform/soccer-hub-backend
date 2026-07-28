package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface TrialLocationPort {

    /**
     * Retrieves the details of a trial location by its unique identifier.
     *
     * @param locationId the unique identifier of the trial location
     * @return a TrialBookingDetailsDto.Location containing the details of the trial location
     */
    TrialBookingDetailsDto.Location getDetails(UUID locationId);

    /**
     * Retrieves the details of multiple trial locations by their unique identifiers.
     *
     * @param locationIds the unique identifiers of the trial locations
     * @return a map of trial location details indexed by their unique identifiers
     */
    Map<UUID, TrialBookingDetailsDto.Location> getDetails(
            Collection<UUID> locationIds
    );
}
