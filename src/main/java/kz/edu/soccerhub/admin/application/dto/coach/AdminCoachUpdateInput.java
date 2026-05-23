package kz.edu.soccerhub.admin.application.dto.coach;

import jakarta.validation.constraints.NotBlank;

public record AdminCoachUpdateInput(
        @NotBlank
        String firstName,
        @NotBlank
        String lastName,
        @NotBlank
        String email,
        String phone,
        String specialization
) {
}
