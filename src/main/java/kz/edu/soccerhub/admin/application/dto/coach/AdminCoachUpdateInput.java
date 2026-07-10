package kz.edu.soccerhub.admin.application.dto.coach;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record AdminCoachUpdateInput(
        @NotBlank
        String firstName,
        @NotBlank
        String lastName,
        @NotBlank
        String email,
        LocalDate birthDate,
        String phone,
        String specialization,
        String description
) {
}
