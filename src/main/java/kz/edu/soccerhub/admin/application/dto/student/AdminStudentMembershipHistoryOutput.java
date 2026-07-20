package kz.edu.soccerhub.admin.application.dto.student;

import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AdminStudentMembershipHistoryOutput(
        Player player,
        List<Item> items
) {
    public record Player(
            UUID id,
            String fullName
    ) {
    }

    public record Item(
            UUID membershipId,
            Group group,
            String status,
            LocalDate joinedAt,
            LocalDate leftAt,
            String joinReason,
            String leaveReason,
            String comment,
            UUID sourceContractId,
            Capabilities capabilities
    ) {
    }

    public record Capabilities(
            boolean canTransfer,
            boolean canRemove
    ) {
    }

    public record Group(
            UUID id,
            String name,
            MediaAssetResponse avatar
    ) {
    }
}
