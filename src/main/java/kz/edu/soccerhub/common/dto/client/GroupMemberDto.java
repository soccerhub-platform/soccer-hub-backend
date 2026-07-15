package kz.edu.soccerhub.common.dto.client;

import java.time.LocalDate;
import java.util.UUID;

public record GroupMemberDto(
        UUID membershipId,
        UUID clientId,
        UUID playerId,
        String childName,
        LocalDate birthDate,
        String contractStatus,
        LocalDate joinedAt,
        LocalDate leftAt
) {
}
