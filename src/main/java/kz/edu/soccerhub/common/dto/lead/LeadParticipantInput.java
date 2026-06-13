package kz.edu.soccerhub.common.dto.lead;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kz.edu.soccerhub.crm.domain.model.enums.Gender;

import java.time.LocalDate;

public record LeadParticipantInput(
        @NotBlank(message = "Participant name is required")
        @Size(max = 255, message = "Participant name is too long")
        String fullName,

        LocalDate birthDate,

        Gender gender,

        @Size(max = 100, message = "Experience is too long")
        String experience
) {
}
