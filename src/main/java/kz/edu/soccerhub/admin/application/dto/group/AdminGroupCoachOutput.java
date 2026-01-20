package kz.edu.soccerhub.admin.application.dto.group;

import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record AdminGroupCoachOutput(
        UUID groupCoachId,
        UUID coachId,
        UUID groupId,
        String coachFirstName,
        String coachLastName,
        LocalDate birthDate,
        String phone,
        String email,
        boolean active,
        CoachRole coachRole,
        LocalDate assignedFrom,
        LocalDate assignedTo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
