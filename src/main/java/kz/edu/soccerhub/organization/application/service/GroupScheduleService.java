package kz.edu.soccerhub.organization.application.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.common.dto.group.*;
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
    public void createSchedule(@NotNull UUID groupId, @Valid GroupScheduleBatchCommand groupScheduleBatchCommand) {
        validateScheduleBatch(
                groupId,
                groupScheduleBatchCommand.coachId(),
                groupScheduleBatchCommand.startDate(),
                groupScheduleBatchCommand.endDate(),
                groupScheduleBatchCommand.slots(),
                List.of()
        );

        List<GroupSchedule> schedules = groupScheduleBatchCommand.slots().stream()
                .map(slot -> GroupSchedule.builder()
                        .id(UUID.randomUUID())
                        .groupId(groupId)
                        .coachId(groupScheduleBatchCommand.coachId())
                        .dayOfWeek(slot.dayOfWeek())
                        .startTime(slot.startTime())
                        .endTime(slot.endTime())
                        .startDate(groupScheduleBatchCommand.startDate())
                        .endDate(groupScheduleBatchCommand.endDate())
                        .scheduleType(groupScheduleBatchCommand.type())
                        .status(ScheduleStatus.ACTIVE)
                        .build())
                .toList();

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
    }

    @Override
    @Transactional
    public void updateScheduleBatch(
            UUID groupId,
            UpdateScheduleBatchCommand command
    ) {
        // 1. находим ТЕКУЩИЙ batch
        List<GroupSchedule> currentBatch =
                groupScheduleRepository
                        .findBatch(
                                groupId,
                                command.coachId(),
                                command.type(),
                                command.startDate(),
                                command.endDate(),
                                ScheduleStatus.ACTIVE
                        );

        if (currentBatch.isEmpty()) {
            throw new NotFoundException(
                    "Schedule batch not found",
                    Map.of(
                            "groupId", groupId,
                            "coachId", command.coachId(),
                            "type", command.type(),
                            "startDate", command.startDate(),
                            "endDate", command.endDate()
                    )
            );
        }

        // 2. ids текущего batch — их игнорируем при валидации
        List<UUID> ignoreIds = currentBatch.stream()
                .map(GroupSchedule::getId)
                .toList();

        // 3. валидация нового batch
        validateScheduleBatch(
                groupId,
                command.coachId(),
                command.startDate(),
                command.endDate(),
                command.slots(),
                ignoreIds
        );

        // 4. закрываем СТАРЫЙ batch
        currentBatch.forEach(s -> s.setStatus(ScheduleStatus.CANCELLED));

        // 5. создаём НОВЫЙ batch
        List<GroupSchedule> newBatch = command.slots().stream()
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

        groupScheduleRepository.saveAll(newBatch);
    }

    @Override
    @Transactional
    public void cancelScheduleFromDate(@NotNull UUID scheduleId, @NotNull LocalDate cancelFrom) {
        GroupSchedule groupSchedule = groupScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("Group schedule not found", scheduleId));

        if (cancelFrom.isBefore(groupSchedule.getStartDate())) {
            throw new BadRequestException("Cancel date is before schedule start", cancelFrom);
        }

        groupSchedule.setEndDate(cancelFrom.minusDays(1));

        if (groupSchedule.getEndDate().isBefore(groupSchedule.getStartDate())) {
            groupSchedule.setStatus(ScheduleStatus.CANCELLED);
        }
    }

    @Transactional
    public void cancelScheduleBatch(
            UUID groupId,
            CancelScheduleBatchCommand command
    ) {
        List<GroupSchedule> batch =
                groupScheduleRepository.findBatch(
                        groupId,
                        command.coachId(),
                        command.type(),
                        command.startDate(),
                        command.endDate(),
                        ScheduleStatus.ACTIVE
                );

        if (batch.isEmpty()) {
            throw new NotFoundException("Schedule batch not found", command);
        }

        batch.forEach(s -> s.setStatus(ScheduleStatus.CANCELLED));
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
                    while (date.getDayOfWeek() != s.getDayOfWeek()) {
                        date = date.plusDays(1);
                    }
                    LocalDateTime dt = LocalDateTime.of(date, s.getStartTime());
                    if (dt.isBefore(LocalDateTime.now())) {
                        dt = dt.plusWeeks(1);
                    }
                    return Stream.of(dt);
                })
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    private void validateScheduleBatch(
            UUID groupId,
            UUID coachId,
            LocalDate startDate,
            LocalDate endDate,
            List<DayScheduleSlot> slots,
            List<UUID> ignoreScheduleIds
    ) {
        if (slots == null || slots.isEmpty()) {
            throw new BadRequestException("Schedule slots cannot be empty", groupId);
        }

        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date must be before end date", startDate, endDate);
        }

        for (DayScheduleSlot slot : slots) {
            if (slot.startTime().isAfter(slot.endTime())) {
                throw new BadRequestException(
                        "Invalid time range",
                        slot.dayOfWeek(),
                        slot.startTime(),
                        slot.endTime()
                );
            }
        }

        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                DayScheduleSlot a = slots.get(i);
                DayScheduleSlot b = slots.get(j);
                if (a.dayOfWeek() == b.dayOfWeek()
                        && timeOverlaps(a.startTime(), a.endTime(), b.startTime(), b.endTime())) {
                    throw new BadRequestException("Overlapping slots in schedule", a.dayOfWeek());
                }
            }
        }

        for (DayScheduleSlot slot : slots) {
            List<GroupSchedule> existing =
                    groupScheduleRepository
                            .findByGroupIdAndDayOfWeekAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                    groupId,
                                    slot.dayOfWeek(),
                                    ScheduleStatus.ACTIVE,
                                    endDate,
                                    startDate
                            );

            for (GroupSchedule ex : existing) {
                if (ignoreScheduleIds.contains(ex.getId())) {
                    continue;
                }
                if (timeOverlaps(
                        ex.getStartTime(),
                        ex.getEndTime(),
                        slot.startTime(),
                        slot.endTime()
                )) {
                    throw new BadRequestException(
                            "Schedule conflicts with existing group schedule",
                            Map.of(
                                    "dayOfWeek", slot.dayOfWeek(),
                                    "existing", ex.getStartTime() + "-" + ex.getEndTime(),
                                    "requested", slot.startTime() + "-" + slot.endTime()
                            )
                    );
                }
            }
        }

        for (DayScheduleSlot slot : slots) {
            List<GroupSchedule> conflicts =
                    groupScheduleRepository.findCoachConflicts(
                            coachId,
                            slot.dayOfWeek(),
                            startDate,
                            endDate
                    );

            for (GroupSchedule ex : conflicts) {
                if (ignoreScheduleIds.contains(ex.getId())) {
                    continue;
                }
                if (timeOverlaps(
                        ex.getStartTime(),
                        ex.getEndTime(),
                        slot.startTime(),
                        slot.endTime()
                )) {
                    throw new BadRequestException(
                            "Coach has schedule conflict",
                            Map.of(
                                    "coachId", coachId,
                                    "groupId", ex.getGroupId(),
                                    "dayOfWeek", slot.dayOfWeek(),
                                    "existing", ex.getStartTime() + "-" + ex.getEndTime()
                            )
                    );
                }
            }
        }
    }

    private boolean timeOverlaps(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        return s1.isBefore(e2) && s2.isBefore(e1);
    }
}