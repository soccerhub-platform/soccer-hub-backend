package kz.edu.soccerhub.common.dto.lead;

import java.util.List;
import java.util.UUID;

public record LeadCreateOutput(
        List<UUID> leadIds
) {
    public UUID primaryLeadId() {
        return leadIds.getFirst();
    }
}