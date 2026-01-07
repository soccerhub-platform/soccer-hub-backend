package kz.edu.soccerhub.common.dto.group;

import kz.edu.soccerhub.organization.domain.model.enums.GroupLevel;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import lombok.Builder;

import java.util.UUID;

@Builder
public record GroupDto(
        UUID groupId,
        String name,
        UUID branchId,
        Integer ageFrom,
        Integer ageTo,
        GroupLevel level,
        Integer capacity,
        String description,
        GroupStatus status
) {}