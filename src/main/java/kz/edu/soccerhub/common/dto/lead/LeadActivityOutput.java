package kz.edu.soccerhub.common.dto.lead;

import java.time.LocalDateTime;

public record LeadActivityOutput(
        String type,
        String description,
        LocalDateTime createdAt,
        String actorName
) {
}

