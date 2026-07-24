package kz.edu.soccerhub.common.dto.lead;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationshipType;

import java.time.LocalDate;
import java.util.UUID;

public record ConvertLeadRequest(
        @NotNull(message = "participantId is required")
        UUID participantId,

        @NotNull(message = "participantBirthDate is required")
        LocalDate participantBirthDate,

        @NotNull(message = "relationshipType is required")
        ClientStudentRelationshipType relationshipType,

        boolean replacePrimaryContact,
        boolean replacePrimaryPayer
) {
}
