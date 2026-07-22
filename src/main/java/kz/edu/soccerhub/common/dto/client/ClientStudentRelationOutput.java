package kz.edu.soccerhub.common.dto.client;

import java.time.LocalDate;
import java.util.UUID;

public record ClientStudentRelationOutput(
        UUID id,
        UUID clientId,
        String clientName,
        UUID playerId,
        String playerName,
        ClientStudentRelationshipType relationshipType,
        boolean primaryContact,
        boolean primaryPayer,
        boolean legalRepresentative,
        boolean receivesNotifications,
        LocalDate startedAt,
        LocalDate endedAt,
        boolean active
) {
}
