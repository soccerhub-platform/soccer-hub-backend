package kz.edu.soccerhub.common.dto.client;

import java.util.UUID;

public record ClientWorkspaceStatusCommand(
        UUID clientId,
        String status
) {
}
