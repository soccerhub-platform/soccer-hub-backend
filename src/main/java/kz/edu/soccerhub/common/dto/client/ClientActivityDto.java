package kz.edu.soccerhub.common.dto.client;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ClientActivityDto(
        UUID id,
        String type,
        LocalDateTime occurredAt,
        UUID actorUserId,
        Map<String, Object> payload
) {
}
