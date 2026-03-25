package kz.edu.soccerhub.common.dto.lead;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record LeadCreateCommand(

        @NotBlank(message = "Parent name is required")
        @Size(max = 255, message = "Parent name is too long")
        String parentName,

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

        UUID assignedAdminId,

        @NotNull(message = "Branch id is required")
        UUID branchId,

        @Valid
        @NotEmpty(message = "At least one child is required")
        List<LeadChildInput> children
) {}