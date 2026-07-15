package kz.edu.soccerhub.admin.application.dto.group;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AdminRemoveGroupMembershipInput(
        @NotNull LocalDate leftAt,
        String reason,
        String comment
) {
}
