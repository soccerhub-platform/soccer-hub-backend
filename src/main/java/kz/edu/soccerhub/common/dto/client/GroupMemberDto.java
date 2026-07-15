package kz.edu.soccerhub.common.dto.client;

import java.time.LocalDate;
import java.util.UUID;

public record GroupMemberDto(
        UUID membershipId,
        UUID clientId,
        UUID playerId,
        String childName,
        LocalDate birthDate,
        String membershipStatus,
        String contractStatus,
        UUID contractId,
        String contractNumber,
        LocalDate contractStartDate,
        LocalDate contractEndDate,
        LocalDate joinedAt,
        LocalDate leftAt
) {
    public GroupMemberDto(
            UUID membershipId,
            UUID clientId,
            UUID playerId,
            String childName,
            LocalDate birthDate,
            String membershipStatus,
            String contractStatus,
            LocalDate joinedAt,
            LocalDate leftAt
    ) {
        this(
                membershipId,
                clientId,
                playerId,
                childName,
                birthDate,
                membershipStatus,
                contractStatus,
                null,
                null,
                null,
                null,
                joinedAt,
                leftAt
        );
    }
}
