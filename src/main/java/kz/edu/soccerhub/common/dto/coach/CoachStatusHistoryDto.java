package kz.edu.soccerhub.common.dto.coach;

import java.time.LocalDateTime;
import java.util.UUID;

public record CoachStatusHistoryDto(
        String status,
        LocalDateTime changedAt,
        UUID changedBy
) {
}
