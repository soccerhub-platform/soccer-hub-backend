package kz.edu.soccerhub.admin.application.dto.student;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AdminStudentUpdateInput(
        @NotBlank(message = "First name must not be blank")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,
        @NotBlank(message = "Last name must not be blank")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,
        @Size(max = 100, message = "Position must not exceed 100 characters")
        String position
) {
}
