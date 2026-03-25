package kz.edu.soccerhub.common.dto.lead;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.edu.soccerhub.crm.domain.model.enums.Gender;

public record LeadChildInput(
        @NotBlank(message = "Child name is required")
        @Size(max = 255, message = "Child name is too long")
        String childName,

        @NotNull(message = "Child age is required")
        Integer childAge,

        Gender gender,

        @Size(max = 100, message = "Experience is too long")
        String experience
) {
}

