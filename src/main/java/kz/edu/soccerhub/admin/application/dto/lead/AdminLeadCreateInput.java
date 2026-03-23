package kz.edu.soccerhub.admin.application.dto.lead;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AdminLeadCreateInput(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name is too long")
        String name,

        @NotBlank(message = "Phone is required")
        @Pattern(
                regexp = "^[0-9+()\\-\\s]{7,20}$",
                message = "Invalid phone format"
        )
        String phone,

        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email is too long")
        String email,

        @Size(max = 1000, message = "Comment is too long")
        String comment,

        @NotNull(message = "Branch id is required")
        UUID branchId
) {
}
