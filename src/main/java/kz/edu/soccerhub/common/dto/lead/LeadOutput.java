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
        String childName,
        Integer childAge,
        LeadSource source,
        LeadStatus status,
        UUID assignedAdminId,
        String comment,
        JsonNode qualificationData,
        List<LeadChildOutput> children,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

