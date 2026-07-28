package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface TrialCoachPort {

    /**
     * Retrieves the details of a trial coach by their unique identifier.
     *
     * @param coachId the unique identifier of the trial coach
     * @return the details of the trial coach
     */
    TrialBookingDetailsDto.Coach getDetails(UUID coachId);

    /**
     * Retrieves the details of multiple trial coaches by their unique identifiers.
     *
     * @param coachIds the unique identifiers of the trial coaches
     * @return a map containing the details of the trial coaches
     */
    Map<UUID, TrialBookingDetailsDto.Coach> getDetails(
            Collection<UUID> coachIds
    );
}
