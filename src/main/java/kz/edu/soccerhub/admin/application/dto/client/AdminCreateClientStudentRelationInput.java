package kz.edu.soccerhub.admin.application.dto.client;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationshipType;

import java.time.LocalDate;
import java.util.UUID;

public record AdminCreateClientStudentRelationInput(
        @NotNull UUID playerId,
        @NotNull ClientStudentRelationshipType relationshipType,
        boolean primaryContact,
        boolean primaryPayer,
        boolean legalRepresentative,
        boolean receivesNotifications,
        @NotNull LocalDate startedAt
) {
}
