package kz.edu.soccerhub.admin.application.dto.group;

import java.time.LocalDate;
import java.util.UUID;

public record AdminGroupMemberOutput(
        UUID membershipId,
        UUID clientId,
        UUID playerId,
        String childName,
        LocalDate birthDate,
        int attendanceRate,
        String membershipStatus,
        String contractStatus,
        LocalDate joinedAt,
        LocalDate leftAt,
        Capabilities capabilities
) {
    public record Capabilities(
            boolean canTransfer,
            boolean canRemove
    ) {
    }
}
