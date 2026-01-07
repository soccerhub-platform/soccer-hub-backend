package kz.edu.soccerhub.organization.application.dto;

import kz.edu.soccerhub.organization.domain.model.enums.ScheduleStatus;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record ScheduleSearchCriteria(
        UUID groupId,
        UUID coachId,
        UUID branchId,

        LocalDate fromDate,
        LocalDate toDate,

        DayOfWeek dayOfWeek,
        ScheduleStatus status
) {}