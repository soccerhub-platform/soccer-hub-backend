package kz.edu.soccerhub.coach.application.dto.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CoachProfileUpdateRequest(
        @NotBlank(message = "firstName is required")
        String firstName,
        @NotBlank(message = "lastName is required")
        String lastName,
        @NotBlank(message = "phone is required")
        String phone,
        @Email(message = "email must be valid")
        @NotBlank(message = "email is required")
        String email,
        @Size(max = 255, message = "specialization max length is 255")
        String specialization,
        @Size(max = 1000, message = "bio max length is 1000")
        String bio
) {
}
