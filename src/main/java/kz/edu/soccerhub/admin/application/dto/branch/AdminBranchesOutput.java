package kz.edu.soccerhub.admin.application.dto.branch;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AdminBranchesOutput(
        UUID branchId,
        String name,
        String address,
        UUID clubId
) {
}
