package kz.edu.soccerhub.common.dto.coach;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CoachStatusHistoryDto(
        String status,
        LocalDateTime changedAt,
        UUID changedBy,
        String eventType,
        String previousAccountStatus,
        String newAccountStatus,
        String previousWorkStatus,
        String newWorkStatus,
        String reason,
        LocalDate vacationFrom,
        LocalDate vacationTo
) {
    public CoachStatusHistoryDto(
            String status,
            LocalDateTime changedAt,
            UUID changedBy,
            String eventType,
            String previousAccountStatus,
            String newAccountStatus,
            String previousWorkStatus,
            String newWorkStatus,
            String reason
    ) {
        this(
                status,
                changedAt,
                changedBy,
                eventType,
                previousAccountStatus,
                newAccountStatus,
                previousWorkStatus,
                newWorkStatus,
                reason,
                null,
                null
        );
    }
}
