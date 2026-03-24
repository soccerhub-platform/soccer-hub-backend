package kz.edu.soccerhub.common.dto.lead;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record LeadQualificationInput(
        @Valid
        List<LeadChildInput> children,

        @Size(max = 255, message = "Preferred days value is too long")
        String preferredDays,

        @Size(max = 100, message = "Experience value is too long")
        String experience,

        @Size(max = 1000, message = "Notes is too long")
        String notes
) {
}

