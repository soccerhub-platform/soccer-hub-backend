package kz.edu.soccerhub.organization.application.mapper;

import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.organization.domain.model.Group;

public final class GroupMapper {

    private GroupMapper() {}

    public static GroupDto toDto(Group group) {
        return GroupDto.builder()
                .groupId(group.getId())
                .name(group.getName())
                .branchId(group.getBranchId())
                .ageFrom(group.getAgeFrom())
                .ageTo(group.getAgeTo())
                .level(group.getLevel())
                .capacity(group.getCapacity())
                .description(group.getDescription())
                .status(group.getStatus())
                .build();
    }
}