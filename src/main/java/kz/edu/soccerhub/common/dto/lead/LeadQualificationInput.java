package kz.edu.soccerhub.common.dto.lead;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import kz.edu.soccerhub.crm.domain.model.enums.TimePreference;

import java.util.List;

public record LeadQualificationInput(
        @Valid
        List<LeadParticipantInput> participants,

        @Size(max = 255, message = "Preferred days value is too long")
        String preferredDays,

        TimePreference timePreference,

        @Size(max = 100, message = "Experience value is too long")
        String experience,

        @Size(max = 1000, message = "Notes is too long")
        String notes
) {
}
