package kz.edu.soccerhub.dispacher.application.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record DispatcherAdminAssignBranchInput(
        @NotNull(message = "Branch ID cannot be null")
        UUID branchId
) {
}
