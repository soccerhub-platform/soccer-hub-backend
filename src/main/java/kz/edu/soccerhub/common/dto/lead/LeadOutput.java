package kz.edu.soccerhub.common.dto.lead;

import com.fasterxml.jackson.databind.JsonNode;
import kz.edu.soccerhub.crm.domain.model.enums.LeadSource;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record LeadOutput(
        UUID id,
        String parentName,
        String phone,
        String email,
        LeadSource source,
        LeadStatus status,
        List<LeadActionOutput> actions,
        AdminShortOutput assignedAdmin,
        String comment,
        JsonNode qualificationData,
        String lostReasonCode,
        String lostReasonName,
        String lostComment,
        LocalDateTime lostAt,
        List<LeadChildOutput> children,
        LeadTrialOutput trial,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
