package kz.edu.soccerhub.common.dto.lead;

import kz.edu.soccerhub.crm.domain.model.enums.Gender;

import java.time.LocalDate;
import java.util.UUID;

public record LeadParticipantOutput(
        UUID id,
        String fullName,
        LocalDate birthDate,
        Gender gender,
        String experience
) {
}
