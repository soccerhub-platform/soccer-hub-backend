package kz.edu.soccerhub.organization.application.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.common.dto.group.DayScheduleSlot;
import kz.edu.soccerhub.common.dto.group.GroupScheduleBatchCommand;
import kz.edu.soccerhub.common.dto.group.GroupScheduleDto;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.GroupSchedulePort;
import kz.edu.soccerhub.organization.application.dto.ScheduleSearchCriteria;
import kz.edu.soccerhub.organization.application.mapper.GroupScheduleMapper;
import kz.edu.soccerhub.organization.domain.model.GroupSchedule;
import kz.edu.soccerhub.organization.domain.model.enums.ScheduleStatus;
import kz.edu.soccerhub.organization.domain.repository.GroupScheduleRepository;
import kz.edu.soccerhub.organization.infrastructure.GroupScheduleSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupScheduleService implements GroupSchedulePort {

    private final GroupScheduleRepository groupScheduleRepository;

    @Override
    @Transactional
    public void createSchedule(@NotNull UUID groupId, @Valid GroupScheduleBatchCommand command) {
        validateBatchCommand(groupId, command);
        validateCoachAvailability(groupId, command);

        List<GroupSchedule> schedules = command.slots().stream()
                .map(slot -> GroupSchedule.builder()
                        .id(UUID.randomUUID())
                        .groupId(groupId)
                        .coachId(command.coachId())
                        .dayOfWeek(slot.dayOfWeek())
                        .startTime(slot.startTime())
                        .endTime(slot.endTime())
                        .startDate(command.startDate())
                        .endDate(command.endDate())
                        .scheduleType(command.type())
                        .status(ScheduleStatus.ACTIVE)
                        .build())
                .toList();

        log.info("Creating {} schedule slots for group {}", schedules.size(), groupId);
        groupScheduleRepository.saveAll(schedules);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupScheduleDto> getSchedules(ScheduleSearchCriteria criteria) {

        return groupScheduleRepository.findAll(
                        GroupScheduleSpecification.byCriteria(criteria)
                ).stream()
                .map(GroupScheduleMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void cancelSchedule(@NotNull UUID scheduleId) {
        GroupSchedule groupSchedule = groupScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("Group schedule not found", scheduleId));

        if (groupSchedule.getStatus() == ScheduleStatus.CANCELLED) {
            return;
        }

        groupSchedule.setStatus(ScheduleStatus.CANCELLED);
        log.info("Group Schedule {} cancelled", scheduleId);
    }

    @Override
    @Transactional
    public void cancelScheduleFromDate(
            @NotNull UUID scheduleId,
            @NotNull LocalDate cancelFrom
    ) {
        GroupSchedule groupSchedule = groupScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("Group schedule not found", scheduleId));

        if (cancelFrom.isBefore(groupSchedule.getStartDate())) {
            throw new BadRequestException(
                    "Cancel date is before schedule start",
                    cancelFrom
            );
        }

        groupSchedule.setEndDate(cancelFrom.minusDays(1));

        if (groupSchedule.getEndDate().isBefore(groupSchedule.getStartDate())) {
            groupSchedule.setStatus(ScheduleStatus.CANCELLED);
        }

        log.info("Schedule {} cancelled from {}", scheduleId, cancelFrom);
    }

    @Override
    @Transactional
    public void cancelGroupSchedules(@NotNull UUID groupId) {

        List<GroupSchedule> schedules =
                groupScheduleRepository.findByGroupIdAndStatus(
                        groupId,
                        ScheduleStatus.ACTIVE
                );

        schedules.forEach(s -> s.setStatus(ScheduleStatus.CANCELLED));

        log.info(
                "Cancelled {} schedules for group {}",
                schedules.size(),
                groupId
        );
    }

    @Transactional(readOnly = true)
    public int countSessionsPerWeek(UUID groupId) {
        return groupScheduleRepository.countWeeklySessions(groupId);
    }

    @Transactional(readOnly = true)
    public boolean existsScheduleByStatus(UUID groupId, ScheduleStatus scheduleStatus) {
        return groupScheduleRepository.existsByGroupIdAndStatus(groupId, scheduleStatus);
    }

    @Transactional(readOnly = true)
    public LocalDateTime getNextSession(UUID groupId) {
        List<GroupSchedule> schedules =
                groupScheduleRepository.findByGroupIdAndStatus(
                        groupId,
                        ScheduleStatus.ACTIVE
                );

        return calculateNextSession(schedules);
    }

    private LocalDateTime calculateNextSession(List<GroupSchedule> schedules) {
        LocalDate today = LocalDate.now();

        return schedules.stream()
                .flatMap(s -> {
                    LocalDate date = today;

                    // ищем ближайшую дату, совпадающую с dayOfWeek
                    while (date.getDayOfWeek() != s.getDayOfWeek()) {
                        date = date.plusDays(1);
                    }

                    LocalDateTime dt = LocalDateTime.of(date, s.getStartTime());

                    // если сегодня и уже прошло — берем следующую неделю
                    if (dt.isBefore(LocalDateTime.now())) {
                        dt = dt.plusWeeks(1);
                    }

                    return Stream.of(dt);
                })
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    private void validateBatchCommand(@NotNull UUID groupId, @Valid GroupScheduleBatchCommand command) {
        if (command.slots().isEmpty()) {
            throw new BadRequestException("Schedule slots cannot be empty", groupId);
        }

        if (command.startDate().isAfter(command.endDate())) {
            throw new BadRequestException("Start date must be before end date", command.startDate(), command.endDate());
        }

        for (DayScheduleSlot slot : command.slots()) {
            if (slot.startTime().isAfter(slot.endTime())) {
                throw new BadRequestException("Invalid time range for slot", slot.dayOfWeek());
            }
        }

        validateNoInternalOverlaps(command);
        validateNoOverlapsWithExisting(groupId, command);
    }

    private void validateNoInternalOverlaps(GroupScheduleBatchCommand command) {
        List<DayScheduleSlot> slots = command.slots();
        for (int i = 0; i < slots.size(); i++) {
            DayScheduleSlot slot1 = slots.get(i);
            for (int j = i + 1; j < slots.size(); j++) {
                DayScheduleSlot slot2 = slots.get(j);
                if (slot1.dayOfWeek() == slot2.dayOfWeek() &&
                        timeOverlaps(slot1.startTime(), slot1.endTime(), slot2.startTime(), slot2.endTime())) {
                    throw new BadRequestException("Overlapping schedule slots detected", slot1.dayOfWeek());
                }
            }
        }
    }

    private void validateNoOverlapsWithExisting(
            UUID groupId,
            GroupScheduleBatchCommand command
    ) {
        for (DayScheduleSlot slot : command.slots()) {
            List<GroupSchedule> existingSchedules = groupScheduleRepository
                    .findByGroupIdAndDayOfWeekAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                            groupId,
                            slot.dayOfWeek(),
                            ScheduleStatus.ACTIVE,
                            command.endDate(),
                            command.startDate()
                    );

            for (GroupSchedule existingSchedule : existingSchedules) {
                if (timeOverlaps(
                        existingSchedule.getStartTime(), existingSchedule.getEndTime(),
                        slot.startTime(), slot.endTime()
                )) {
                    throw new BadRequestException(
                            "Schedule conflicts with existing schedule",
                            slot.dayOfWeek(),
                            existingSchedule.getStartTime(),
                            existingSchedule.getEndTime()
                    );
                }
            }
        }
    }

    private void validateCoachAvailability(
            UUID groupId,
            GroupScheduleBatchCommand command
    ) {
        final UUID coachId = command.coachId();

        for (DayScheduleSlot slot : command.slots()) {
            List<GroupSchedule> existingSchedules =
                    groupScheduleRepository.findCoachConflicts(
                            coachId,
                            slot.dayOfWeek(),
                            command.startDate(),
                            command.endDate()
                    );

            for (GroupSchedule existingSchedule : existingSchedules) {
                // если обновляем расписание этой же группы — пропускаем
                if (existingSchedule.getGroupId().equals(groupId)) {
                    continue;
                }

                if (timeOverlaps(
                        existingSchedule.getStartTime(),
                        existingSchedule.getEndTime(),
                        slot.startTime(),
                        slot.endTime()
                )) {
                    throw new BadRequestException(
                            "Coach has schedule conflict",
                            Map.of(
                                    "coachId", coachId,
                                    "dayOfWeek", slot.dayOfWeek(),
                                    "existingGroupId", existingSchedule.getGroupId(),
                                    "existingTime", existingSchedule.getStartTime() + " - " + existingSchedule.getEndTime(),
                                    "requestedTime", slot.startTime() + " - " + slot.endTime()
                            )
                    );
                }
            }
        }
    }

    private boolean timeOverlaps(
            LocalTime s1, LocalTime e1,
            LocalTime s2, LocalTime e2
    ) {
        return s1.isBefore(e2) && s2.isBefore(e1);
    }
}
