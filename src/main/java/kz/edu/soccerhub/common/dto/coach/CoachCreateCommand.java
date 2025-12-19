package kz.edu.soccerhub.common.dto.coach;

import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record CoachCreateCommand(
        UUID id,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String phone,
        String email
) {
}
