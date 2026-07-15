package kz.edu.soccerhub.common.dto.group;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record GroupActivityDto(
        UUID id,
        String type,
        LocalDateTime occurredAt,
        ActorRef actor,
        Map<String, Object> payload
) {
    public record ActorRef(UUID id, String fullName) {
    }
}
