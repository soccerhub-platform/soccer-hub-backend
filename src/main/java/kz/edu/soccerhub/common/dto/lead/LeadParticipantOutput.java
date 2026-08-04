package kz.edu.soccerhub.common.dto.lead;

import kz.edu.soccerhub.crm.domain.model.enums.Gender;
import kz.edu.soccerhub.crm.domain.model.enums.LeadParticipantStage;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record LeadParticipantOutput(
        UUID id,
        String fullName,
        LocalDate birthDate,
        Gender gender,
        String experience,
        LeadParticipantStage stage,
        UUID playerId,
        LocalDateTime stageChangedAt
) {
}