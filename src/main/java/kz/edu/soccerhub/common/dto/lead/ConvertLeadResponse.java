package kz.edu.soccerhub.common.dto.lead;

import java.util.UUID;

public record ConvertLeadResponse(
        UUID leadId,
        UUID clientId,
        UUID playerId,
        UUID contractId,
        String status
) {
}
