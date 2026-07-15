package kz.edu.soccerhub.admin.application.dto.group;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record AdminTransferGroupMembershipInput(
        @NotNull UUID targetGroupId,
        @NotNull LocalDate transferDate,
        String reason,
        String comment
) {
}
