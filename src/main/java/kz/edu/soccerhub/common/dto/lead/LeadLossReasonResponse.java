package kz.edu.soccerhub.common.dto.lead;

import java.util.Set;

public record LeadLossReasonResponse(
        String code,
        String name,
        Set<LeadLossReasonStage> stages
) {
}
