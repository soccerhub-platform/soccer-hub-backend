package kz.edu.soccerhub.common.dto.profile;

import java.util.UUID;

public record BranchRef(
        UUID branchId,
        String branchName
) {
}
