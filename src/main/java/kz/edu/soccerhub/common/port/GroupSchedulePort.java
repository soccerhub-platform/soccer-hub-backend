package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.group.GroupScheduleBatchCommand;
import kz.edu.soccerhub.common.dto.group.GroupScheduleDto;
import kz.edu.soccerhub.organization.application.dto.ScheduleSearchCriteria;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface GroupSchedulePort {

    void createSchedule(UUID groupId, GroupScheduleBatchCommand command);

    void cancelSchedule(UUID scheduleId);

    void cancelScheduleFromDate(UUID scheduleId, LocalDate from);

    void cancelGroupSchedules(UUID groupId);

    List<GroupScheduleDto> getSchedules(ScheduleSearchCriteria criteria);
}