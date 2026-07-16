package kz.edu.soccerhub.coach.application.service;

import kz.edu.soccerhub.coach.domain.repository.CoachAvailabilityRepository;
import kz.edu.soccerhub.common.dto.coach.CoachWorkingAvailability;
import kz.edu.soccerhub.common.port.CoachWorkingAvailabilityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoachWorkingAvailabilityQueryService implements CoachWorkingAvailabilityPort {

    private final CoachAvailabilityRepository coachAvailabilityRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<CoachWorkingAvailability> findWorkingAvailability(UUID coachId) {
        return coachAvailabilityRepository.findById(coachId)
                .map(availability -> new CoachWorkingAvailability(
                        Arrays.stream(availability.getDays().split(","))
                                .map(String::trim)
                                .filter(day -> !day.isBlank())
                                .map(this::toDayOfWeek)
                                .collect(Collectors.toUnmodifiableSet()),
                        availability.getTimeFrom(),
                        availability.getTimeTo(),
                        availability.getTimezone()
                ));
    }

    private DayOfWeek toDayOfWeek(String value) {
        String normalized = value.toUpperCase(Locale.ROOT);
        return Set.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").contains(normalized)
                ? DayOfWeek.of(switch (normalized) {
                    case "MON" -> 1;
                    case "TUE" -> 2;
                    case "WED" -> 3;
                    case "THU" -> 4;
                    case "FRI" -> 5;
                    case "SAT" -> 6;
                    case "SUN" -> 7;
                    default -> throw new IllegalArgumentException("Unsupported day: " + value);
                })
                : DayOfWeek.valueOf(normalized);
    }
}
