package kz.edu.soccerhub.common.dto.lead;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LeadPrimaryContactInput(
        @NotBlank(message = "Primary contact name is required")
        @Size(max = 255, message = "Primary contact name is too long")
        String fullName,

        @NotBlank(message = "Phone is required")
        @Pattern(
                regexp = "^[0-9+()\\-\\s]{7,20}$",
                message = "Invalid phone format"
        )
        String phone,

        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email is too long")
        String email
) {
}
