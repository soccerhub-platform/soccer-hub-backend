package kz.edu.soccerhub.admin.application.dto.group;

import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AdminGroupMemberCandidatesOutput(
        UUID groupId,
        List<Item> items,
        int total,
        int page,
        int size
) {
    public record Item(
            UUID playerId,
            String fullName,
            LocalDate birthDate,
            Integer age,
            boolean eligible,
            LocalDate earliestAvailableJoinDate,
            List<Warning> warnings,
            List<CurrentMembership> currentMemberships
    ) {
    }

    public record Warning(
            String code,
            String message
    ) {
    }

    public record CurrentMembership(
            UUID membershipId,
            UUID groupId,
            String groupName,
            MediaAssetResponse groupAvatar,
            String status,
            LocalDate joinedAt,
            LocalDate leftAt
    ) {
    }
}
