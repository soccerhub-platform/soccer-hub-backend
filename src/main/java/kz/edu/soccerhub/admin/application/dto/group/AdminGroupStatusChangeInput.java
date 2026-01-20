package kz.edu.soccerhub.admin.application.dto.group;

import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import lombok.Builder;

@Builder
public record AdminGroupStatusChangeInput(
        GroupStatus status
) {
}
