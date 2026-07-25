package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;

import java.util.UUID;

public interface TrialLocationPort {

    TrialBookingDetailsDto.Location getDetails(UUID locationId);
}
