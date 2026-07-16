package kz.edu.soccerhub.admin.application.dto.group;

import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminGroupCoachRemovalPreviewOutput(
        UUID groupCoachId,
        UUID groupId,
        UUID coachId,
        String coachName,
        CoachRole role,
        int activeScheduleSlots,
        int futureSessionsCount,
        LocalDateTime nextSessionAt,
        boolean replacementRequired,
        boolean canRemove,
        List<ReplacementCandidate> replacementCandidates
) {
    public record ReplacementCandidate(
            UUID groupCoachId,
            UUID coachId,
            String coachName,
            CoachRole role
    ) {}
}
