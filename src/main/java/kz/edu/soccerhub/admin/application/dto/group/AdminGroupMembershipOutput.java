package kz.edu.soccerhub.admin.application.dto.group;

import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;

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
    public record GroupRef(UUID id, String name, MediaAssetResponse avatar) {
        public GroupRef(UUID id, String name) {
            this(id, name, null);
        }
    }

    public record PlayerRef(UUID id, String fullName, LocalDate birthDate) {
    }
}
