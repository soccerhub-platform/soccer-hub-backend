package kz.edu.soccerhub.common.dto.client;

import java.time.LocalDate;
import java.util.UUID;

public record ClientStudentCreateCommand(
        UUID clientId,
        String firstName,
        String lastName,
        LocalDate birthDate,
        ClientStudentRelationshipType relationshipType,
        boolean primaryContact,
        boolean primaryPayer,
        boolean legalRepresentative,
        boolean receivesNotifications,
        LocalDate startedAt
) {
}
