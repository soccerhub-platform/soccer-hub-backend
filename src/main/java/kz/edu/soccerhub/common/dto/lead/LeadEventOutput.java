package kz.edu.soccerhub.common.dto.lead;

import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;

import java.util.UUID;

public record LeadEventOutput(
        UUID leadId,
        LeadStatus status,
        LeadOutput lead
) {
}
