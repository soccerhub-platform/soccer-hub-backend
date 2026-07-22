package kz.edu.soccerhub.common.dto.client;

import java.util.UUID;

public record ClientWorkspaceCreateCommand(
        UUID branchId,
        String firstName,
        String lastName,
        String phone,
        String email,
        String source,
        String comments
) {
}
