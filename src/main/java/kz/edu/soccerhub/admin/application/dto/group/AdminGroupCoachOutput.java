package kz.edu.soccerhub.admin.application.dto.group;

import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;
import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Builder
public record AdminGroupCoachOutput(
        UUID groupCoachId,
        UUID coachId,
        UUID groupId,
        String coachFirstName,
        String coachLastName,
        MediaAssetResponse avatar,
        LocalDate birthDate,
        String phone,
        String email,
        String specialization,
        boolean active,
        String accountStatus,
        String workStatus,
        CoachRole coachRole,
        LocalDate assignedFrom,
        LocalDate assignedTo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Load load,
        NextSession nextSession,
        Capabilities capabilities
) {
    public record Load(
            int groupsCount,
            int weeklySessionsCount,
            int maxWeeklySessions,
            int percentage,
            String status
    ) {}

    public record NextSession(
            UUID sessionId,
            LocalDate sessionDate,
            LocalTime startsAt,
            LocalTime endsAt,
            String status
    ) {}

    public record Capabilities(
            boolean canOpenProfile,
            boolean canChangeRole,
            boolean canSetMain,
            boolean canSetAssistant,
            boolean canUnassign
    ) {}
}
