package kz.edu.soccerhub.coach.infrastructure;

import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.TrialCoachPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

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
}
