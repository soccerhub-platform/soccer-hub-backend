package kz.edu.soccerhub.common.dto.lead;

import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;

import java.util.UUID;

public record ConvertLeadResponse(
        UUID leadId,
        LeadStatus leadStatus,
        UUID clientId,
        String clientName,
        UUID playerId,
        String playerName,
        String status
) {
}
