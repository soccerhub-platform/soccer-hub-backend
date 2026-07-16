package kz.edu.soccerhub.admin.application.dto.group;

import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;
import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminGroupCoachHistoryOutput(
        UUID groupCoachId,
        CoachRef coach,
        CoachRole role,
        LocalDate assignedFrom,
        LocalDate assignedTo,
        boolean active,
        String removalReason,
        CoachRef replacementCoach,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record CoachRef(
            UUID id,
            String firstName,
            String lastName,
            MediaAssetResponse avatar
    ) {}
}
