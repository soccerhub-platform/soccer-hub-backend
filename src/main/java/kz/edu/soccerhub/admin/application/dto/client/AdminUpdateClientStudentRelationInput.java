package kz.edu.soccerhub.admin.application.dto.client;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationshipType;

public record AdminUpdateClientStudentRelationInput(
        @NotNull ClientStudentRelationshipType relationshipType,
        boolean primaryContact,
        boolean primaryPayer,
        boolean legalRepresentative,
        boolean receivesNotifications
) {
}
