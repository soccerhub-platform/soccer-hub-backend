package kz.edu.soccerhub.admin.application.dto.group;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record AdminAddGroupMemberInput(
        @NotNull UUID playerId,
        @NotNull LocalDate joinedAt,
        String reason,
        String comment
) {
}
