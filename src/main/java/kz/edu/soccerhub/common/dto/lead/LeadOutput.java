package kz.edu.soccerhub.common.dto.lead;

import com.fasterxml.jackson.databind.JsonNode;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import kz.edu.soccerhub.crm.domain.model.enums.LeadSource;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.domain.model.enums.TimePreference;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record LeadOutput(
        UUID id,
        LeadType leadType,
        LeadPrimaryContactOutput primaryContact,
        LeadSource source,
        LeadStatus status,
        List<LeadActionOutput> actions,
        AdminShortOutput assignedAdmin,
        String comment,
        JsonNode qualificationData,
        String preferredDays,
        TimePreference timePreference,
        String experience,
        String notes,
        String lostReasonCode,
        String lostReasonName,
        String lostComment,
        LocalDateTime lostAt,
        UUID clientId,
        UUID participantId,
        UUID contractId,
        List<LeadParticipantOutput> participants,
        LeadTrialOutput trial,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
