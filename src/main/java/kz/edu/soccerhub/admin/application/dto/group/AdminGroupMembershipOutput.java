package kz.edu.soccerhub.admin.application.dto.group;

import java.time.LocalDate;
import java.util.UUID;

public record AdminGroupMembershipOutput(
        UUID membershipId,
        GroupRef group,
        PlayerRef player,
        String status,
        LocalDate joinedAt,
        LocalDate leftAt,
        String joinReason,
        String leaveReason,
        String comment,
        UUID sourceContractId
) {
    public record GroupRef(UUID id, String name) {
    }

    public record PlayerRef(UUID id, String fullName, LocalDate birthDate) {
    }
}
