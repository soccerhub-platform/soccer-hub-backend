package kz.edu.soccerhub.coach.infrastructure;

import kz.edu.soccerhub.coach.domain.model.TrainingSession;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.TrialCoachPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CoachTrialAdapter implements TrialCoachPort {

    private final CoachPort coachPort;

    @Override
    public TrialBookingDetailsDto.Coach getDetails(UUID coachId) {
        CoachDto coach = coachPort.getCoach(coachId);

        if (coach == null) {
            throw new NotFoundException("Coach not found", coachId);
        }

        return TrialBookingDetailsDto.Coach.builder()
                .id(coach.id())
                .fullName(coach.firstName() + " " + coach.lastName())
                .build();
    }

    @Override
    public Map<UUID, TrialBookingDetailsDto.Coach> getDetails(Collection<UUID> coachIds) {
        if (coachIds.isEmpty()) {
            return Map.of();
        }

        return coachPort.getCoaches(Set.copyOf(coachIds)).stream()
                .collect(Collectors.toMap(
                        CoachDto::id,
                        this::toContext
                ));
    }

    private TrialBookingDetailsDto.Coach toContext(CoachDto coach) {
        return TrialBookingDetailsDto.Coach.builder()
                .id(coach.id())
                .fullName(coach.firstName() + " " + coach.lastName())
                .build();
    }
}
