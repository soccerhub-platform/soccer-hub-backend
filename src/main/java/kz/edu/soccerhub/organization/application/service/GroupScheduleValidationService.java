package kz.edu.soccerhub.organization.application.service;

import kz.edu.soccerhub.common.dto.group.*;
import kz.edu.soccerhub.organization.domain.model.GroupSchedule;
import kz.edu.soccerhub.organization.domain.model.enums.ScheduleStatus;
import kz.edu.soccerhub.organization.domain.repository.GroupScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupScheduleValidationService {

    private final GroupScheduleRepository groupScheduleRepository;

    @Transactional(readOnly = true)
    public ScheduleValidationResult validate(UUID groupId, GroupScheduleValidationCommand command) {
        List<ScheduleValidationConflict> conflicts = new ArrayList<>();
        List<UUID> ignoreScheduleIds = command.excludeScheduleIds() == null ? List.of() : command.excludeScheduleIds();

        if (command.slots() == null || command.slots().isEmpty()) {
            conflicts.add(new ScheduleValidationConflict(
                    ScheduleValidationConflictCode.EMPTY_SLOTS,
                    command.coachId(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "Schedule slots cannot be empty"
            ));
            return ScheduleValidationResult.of(conflicts);
        }

        if (command.startDate().isAfter(command.endDate())) {
            conflicts.add(new ScheduleValidationConflict(
                    ScheduleValidationConflictCode.INVALID_DATE_RANGE,
                    command.coachId(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    command.startDate(),
                    command.endDate(),
                    "Start date must be before end date"
            ));
        }

        for (DayScheduleSlot slot : command.slots()) {
            if (!slot.startTime().isBefore(slot.endTime())) {
                conflicts.add(new ScheduleValidationConflict(
                        ScheduleValidationConflictCode.INVALID_TIME_RANGE,
                        command.coachId(),
                        slot.dayOfWeek(),
                        slot.startTime(),
                        slot.endTime(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Invalid time range"
                ));
            }
        }

        for (int i = 0; i < command.slots().size(); i++) {
            for (int j = i + 1; j < command.slots().size(); j++) {
                DayScheduleSlot a = command.slots().get(i);
                DayScheduleSlot b = command.slots().get(j);
                if (a.dayOfWeek() == b.dayOfWeek() && timeOverlaps(a.startTime(), a.endTime(), b.startTime(), b.endTime())) {
                    conflicts.add(new ScheduleValidationConflict(
                            ScheduleValidationConflictCode.OVERLAPPING_INPUT_SLOTS,
                            command.coachId(),
                            a.dayOfWeek(),
                            a.startTime(),
                            a.endTime(),
                            max(a.startTime(), b.startTime()),
                            min(a.endTime(), b.endTime()),
                            null,
                            null,
                            null,
                            null,
                            null,
                            "Overlapping slots in schedule input"
                    ));
                }
            }
        }

        for (DayScheduleSlot slot : command.slots()) {
            List<GroupSchedule> existing = groupScheduleRepository
                    .findByGroupIdAndDayOfWeekAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                            groupId,
                            slot.dayOfWeek(),
                            ScheduleStatus.ACTIVE,
                            command.endDate(),
                            command.startDate()
                    );

            for (GroupSchedule ex : existing) {
                if (ignoreScheduleIds.contains(ex.getId())) {
                    continue;
                }
                if (timeOverlaps(ex.getStartTime(), ex.getEndTime(), slot.startTime(), slot.endTime())) {
                    conflicts.add(new ScheduleValidationConflict(
                            ScheduleValidationConflictCode.GROUP_SCHEDULE_CONFLICT,
                            command.coachId(),
                            slot.dayOfWeek(),
                            slot.startTime(),
                            slot.endTime(),
                            max(ex.getStartTime(), slot.startTime()),
                            min(ex.getEndTime(), slot.endTime()),
                            ex.getGroupId(),
                            ex.getGroup() == null ? null : ex.getGroup().getName(),
                            ex.getId(),
                            ex.getStartDate(),
                            ex.getEndDate(),
                            "Schedule conflicts with existing group schedule"
                    ));
                }
            }
        }

        for (DayScheduleSlot slot : command.slots()) {
            List<GroupSchedule> existing = groupScheduleRepository.findCoachConflicts(
                    command.coachId(),
                    slot.dayOfWeek(),
                    command.startDate(),
                    command.endDate()
            );

            for (GroupSchedule ex : existing) {
                if (ignoreScheduleIds.contains(ex.getId())) {
                    continue;
                }
                if (timeOverlaps(ex.getStartTime(), ex.getEndTime(), slot.startTime(), slot.endTime())) {
                    conflicts.add(new ScheduleValidationConflict(
                            ScheduleValidationConflictCode.COACH_SCHEDULE_CONFLICT,
                            command.coachId(),
                            slot.dayOfWeek(),
                            slot.startTime(),
                            slot.endTime(),
                            max(ex.getStartTime(), slot.startTime()),
                            min(ex.getEndTime(), slot.endTime()),
                            ex.getGroupId(),
                            ex.getGroup() == null ? null : ex.getGroup().getName(),
                            ex.getId(),
                            ex.getStartDate(),
                            ex.getEndDate(),
                            "Coach already has a session at this time"
                    ));
                }
            }
        }

        return ScheduleValidationResult.of(conflicts);
    }

    private boolean timeOverlaps(LocalTime startOne, LocalTime endOne, LocalTime startTwo, LocalTime endTwo) {
        return startOne.isBefore(endTwo) && startTwo.isBefore(endOne);
    }

    private LocalTime max(LocalTime one, LocalTime two) {
        return one.isAfter(two) ? one : two;
    }

    private LocalTime min(LocalTime one, LocalTime two) {
        return one.isBefore(two) ? one : two;
    }
}
