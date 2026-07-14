package kz.edu.soccerhub.common.dto.group;

import kz.edu.soccerhub.organization.domain.model.enums.GroupAudienceType;
import kz.edu.soccerhub.organization.domain.model.enums.GroupLevel;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UpdateGroupCommand(
        String name,
        String description,
        UUID branchId,
        Integer ageFrom,
        Integer ageTo,
        GroupAudienceType audienceType,
        Integer capacity,
        GroupLevel level
) {
}
