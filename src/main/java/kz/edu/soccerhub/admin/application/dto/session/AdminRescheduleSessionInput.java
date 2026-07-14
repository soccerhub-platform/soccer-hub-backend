package kz.edu.soccerhub.admin.application.dto.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminRescheduleSessionInput(
        @NotNull
        LocalDateTime startsAt,
        @NotNull
        LocalDateTime endsAt,
        UUID locationId,
        @NotBlank
        String reason
) {
}
