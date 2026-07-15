package kz.edu.soccerhub.admin.application.dto.group;

import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;

import java.time.LocalDate;
import java.util.UUID;

public record AdminGroupMemberOutput(
        UUID membershipId,
        UUID clientId,
        UUID playerId,
        String childName,
        LocalDate birthDate,
        Integer age,
        MediaAssetResponse avatar,
        int attendanceRate,
        String membershipStatus,
        String contractStatus,
        LocalDate joinedAt,
        LocalDate leftAt,
        Player player,
        Membership membership,
        Contract contract,
        Stats stats,
        Capabilities capabilities
) {
    public record Player(
            UUID id,
            String fullName,
            LocalDate birthDate,
            Integer age,
            MediaAssetResponse avatar
    ) {
    }

    public record Membership(
            UUID id,
            String status,
            LocalDate joinedAt,
            LocalDate leftAt
    ) {
    }

    public record Contract(
            UUID id,
            String number,
            String status,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record Stats(
            int attendanceRate
    ) {
    }

    public record Capabilities(
            boolean canOpenProfile,
            boolean canAddToAnotherGroup,
            boolean canTransfer,
            boolean canRemove
    ) {
    }
}
