package kz.edu.soccerhub.common.dto.lead;

import java.util.UUID;

public record AdminShortOutput(
        UUID id,
        String name,
        String email
) {
}

