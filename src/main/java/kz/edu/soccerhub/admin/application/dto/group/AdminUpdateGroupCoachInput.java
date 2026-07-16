package kz.edu.soccerhub.admin.application.dto.group;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;

public record AdminUpdateGroupCoachInput(
        @NotNull CoachRole role
) {
}
