package kz.edu.soccerhub.common.dto.group;

import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record GroupCoachDto(
        UUID id,
        UUID groupId,
        UUID coachId,
        CoachRole role,
        boolean active,
        LocalDate assignedFrom,
        LocalDate assignedTo,
        LocalDateTime createdAt,
        LocalDateTime updateAt
) {
}
