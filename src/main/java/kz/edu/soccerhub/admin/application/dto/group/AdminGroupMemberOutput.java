package kz.edu.soccerhub.admin.application.dto.group;

import java.time.LocalDate;
import java.util.UUID;

public record AdminGroupMemberOutput(
        UUID clientId,
        UUID playerId,
        String childName,
        LocalDate birthDate,
        int attendanceRate,
        String contractStatus,
        LocalDate joinedAt
) {
}
