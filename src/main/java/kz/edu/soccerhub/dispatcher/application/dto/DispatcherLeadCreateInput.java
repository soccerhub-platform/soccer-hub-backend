package kz.edu.soccerhub.dispatcher.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import kz.edu.soccerhub.common.dto.lead.LeadChildInput;

import java.util.List;
import java.util.UUID;

public record DispatcherLeadCreateInput(
        @NotBlank(message = "Parent name is required")
        @Size(max = 255, message = "Parent name is too long")
        String parentName,

        @NotBlank(message = "Phone is required")
        @Pattern(
                regexp = "^[0-9+()\\-\\s]{7,20}$",
                message = "Invalid phone format"
        )
        String phone,

        @NotNull(message = "Branch id is required")
        UUID branchId,

        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email is too long")
        String email,

        @Size(max = 1000, message = "Comment is too long")
        String comment,

        @Valid
        @NotEmpty(message = "At least one child is required")
        List<LeadChildInput> children
) {
}

