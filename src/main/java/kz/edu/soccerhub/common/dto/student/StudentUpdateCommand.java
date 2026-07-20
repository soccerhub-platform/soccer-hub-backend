package kz.edu.soccerhub.common.dto.student;

import java.time.LocalDate;

public record StudentUpdateCommand(
        String firstName,
        String lastName,
        LocalDate birthDate,
        String position
) {
}
