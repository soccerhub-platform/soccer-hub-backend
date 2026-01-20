package kz.edu.soccerhub.organization.application.mapper;

import kz.edu.soccerhub.common.dto.group.GroupCoachDto;
import kz.edu.soccerhub.organization.domain.model.GroupCoach;

public class GroupCoachMapper {

    private GroupCoachMapper() {}

    public static GroupCoachDto toDto(GroupCoach groupCoach) {
        return GroupCoachDto.builder()
                .id(groupCoach.getId())
                .groupId(groupCoach.getGroupId())
                .coachId(groupCoach.getCoachId())
                .role(groupCoach.getRole())
                .active(groupCoach.isActive())
                .assignedFrom(groupCoach.getAssignedFrom())
                .assignedTo(groupCoach.getAssignedTo())
                .createdAt(groupCoach.getCreatedAt())
                .updateAt(groupCoach.getUpdatedAt())
                .build();
    }

}
