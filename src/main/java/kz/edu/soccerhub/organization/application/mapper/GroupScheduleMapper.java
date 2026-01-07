package kz.edu.soccerhub.organization.application.mapper;

import kz.edu.soccerhub.common.dto.group.GroupScheduleDto;
import kz.edu.soccerhub.organization.domain.model.GroupSchedule;

public final class GroupScheduleMapper {

    private GroupScheduleMapper() {}

    public static GroupScheduleDto toDto(GroupSchedule s) {
        return GroupScheduleDto.builder()
                .scheduleId(s.getId())
                .groupId(s.getGroupId())
                .coachId(s.getCoachId())
                .branchId(s.getGroup().getBranchId())

                .dayOfWeek(s.getDayOfWeek())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())

                .startDate(s.getStartDate())
                .endDate(s.getEndDate())

                .scheduleType(s.getScheduleType().name())
                .status(s.getStatus().name())

                .substitution(s.isSubstitution())
                .substitutionCoachId(s.getSubstitutionCoachId())
                .build();
    }
}