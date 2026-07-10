package kz.edu.soccerhub.common.dto.coach;

import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record CoachUpdateCommand(
        UUID coachId,
        String firstName,
        String lastName,
        String email,
        LocalDate birthDate,
        String phone,
        String specialization,
        String bio
) {
}
