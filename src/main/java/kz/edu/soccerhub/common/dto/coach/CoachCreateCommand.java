package kz.edu.soccerhub.common.dto.coach;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record CoachCreateCommand(
        String firstName,
        String lastName,
        LocalDate birthDate,
        String phone,
        String email
) {
}
