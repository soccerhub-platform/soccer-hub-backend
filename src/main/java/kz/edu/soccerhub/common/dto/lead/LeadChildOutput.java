package kz.edu.soccerhub.common.dto.lead;

import java.util.UUID;

public record LeadChildOutput(
        UUID id,
        String childName,
        Integer childAge
) {
}

