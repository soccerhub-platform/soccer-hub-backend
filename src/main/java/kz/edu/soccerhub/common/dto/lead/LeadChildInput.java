package kz.edu.soccerhub.common.dto.lead;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeadChildInput(
        @NotBlank(message = "Child name is required")
        @Size(max = 255, message = "Child name is too long")
        String childName,

        @Min(value = 3, message = "Child age must be at least 3")
        @Max(value = 18, message = "Child age must be at most 18")
        Integer childAge
) {
}

