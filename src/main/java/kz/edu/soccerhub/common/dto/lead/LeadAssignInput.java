package kz.edu.soccerhub.common.dto.lead;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LeadAssignInput(
        @NotNull(message = "Admin id is required")
        UUID assignedAdminId
) {
}

