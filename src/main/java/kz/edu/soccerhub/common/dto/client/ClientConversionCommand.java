package kz.edu.soccerhub.common.dto.client;

import java.time.LocalDate;
import java.util.UUID;

public record ClientConversionCommand(
        UUID existingClientId,
        String primaryContactName,
        String phone,
        String email,
        UUID branchId,
        String source,
        String comments,
        String participantName,
        LocalDate participantBirthDate,
        ClientStudentRelationshipType relationshipType,
        boolean replacePrimaryContact,
        boolean replacePrimaryPayer
) {
}
