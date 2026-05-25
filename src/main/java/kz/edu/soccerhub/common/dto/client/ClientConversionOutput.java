package kz.edu.soccerhub.common.dto.client;

import java.util.UUID;

public record ClientConversionOutput(
        UUID clientId,
        UUID playerId,
        UUID contractId
) {
}
