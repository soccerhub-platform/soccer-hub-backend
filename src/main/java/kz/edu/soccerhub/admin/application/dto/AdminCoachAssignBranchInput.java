package kz.edu.soccerhub.admin.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record AdminCoachAssignBranchInput(
        @NotNull UUID branchId
) {
}
