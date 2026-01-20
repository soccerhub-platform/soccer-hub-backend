package kz.edu.soccerhub.admin.application.dto.group;

import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;

import java.util.UUID;

public record AdminAssignCoachToGroupInput(
        UUID coachId,
        CoachRole role
) {
}
