package kz.edu.soccerhub.organization.infrastructure;

import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.TrialLocationPort;
import kz.edu.soccerhub.organization.domain.model.Location;
import kz.edu.soccerhub.organization.domain.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LocationTrialAdapter implements TrialLocationPort {

    private final LocationRepository repository;

    @Override
    public TrialBookingDetailsDto.Location getDetails(UUID locationId) {
        Location location = repository.findById(locationId)
                .orElseThrow(() -> new NotFoundException("Location not found", locationId));

        return TrialBookingDetailsDto.Location.builder()
                .id(location.getId())
                .name(location.getName())
                .build();
    }
}
