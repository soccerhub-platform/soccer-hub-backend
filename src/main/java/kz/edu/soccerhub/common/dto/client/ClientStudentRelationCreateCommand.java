package kz.edu.soccerhub.common.dto.client;

import java.time.LocalDate;
import java.util.UUID;

public record ClientStudentRelationCreateCommand(
        UUID clientId,
        UUID playerId,
        ClientStudentRelationshipType relationshipType,
        boolean primaryContact,
        boolean primaryPayer,
        boolean legalRepresentative,
        boolean receivesNotifications,
        LocalDate startedAt
) {
}
