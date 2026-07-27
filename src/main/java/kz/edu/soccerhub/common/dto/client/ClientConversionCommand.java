package kz.edu.soccerhub.common.dto.client;

import kz.edu.soccerhub.client.domain.enums.ClientSource;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record ClientConversionCommand(
        UUID existingClientId,
        String primaryContactName,
        String phone,
        String email,
        UUID branchId,
        ClientSource source,
        String sourceDetails,
        String comments,
        String participantName,
        LocalDate participantBirthDate,
        ClientStudentRelationshipType relationshipType,
        boolean replacePrimaryContact,
        boolean replacePrimaryPayer,
        UUID sourceLeadId,
        UUID actorUserId
) {
}