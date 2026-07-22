package kz.edu.soccerhub.common.dto.client;

import java.util.UUID;

public record ClientStudentRelationUpdateCommand(
        UUID relationId,
        ClientStudentRelationshipType relationshipType,
        boolean primaryContact,
        boolean primaryPayer,
        boolean legalRepresentative,
        boolean receivesNotifications
) {
}
