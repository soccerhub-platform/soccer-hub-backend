package kz.edu.soccerhub.admin.application.dto.coach;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AdminTrainerActivityOutput(
        List<Item> content,
        long totalElements,
        int totalPages,
        int number,
        int size,
        boolean first,
        boolean last,
        boolean empty
) {
    public record Item(
            String id,
            OffsetDateTime occurredAt,
            String type,
            String title,
            Actor actor,
            List<Change> changes,
            Map<String, Object> metadata
    ) {
    }

    public record Actor(
            UUID id,
            String name
    ) {
    }

    public record Change(
            String field,
            String label,
            String from,
            String to,
            String fromLabel,
            String toLabel
    ) {
    }
}