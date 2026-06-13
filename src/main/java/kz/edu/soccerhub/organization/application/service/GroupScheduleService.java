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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupScheduleService implements GroupSchedulePort {

    private final GroupScheduleRepository groupScheduleRepository;
    private final GroupScheduleValidationService groupScheduleValidationService;

    @Override
    @Transactional
    public void createSchedule(@NotNull UUID groupId, @Valid GroupScheduleBatchCommand groupScheduleBatchCommand) {
        ensureValid(groupId, GroupScheduleValidationCommand.builder()
                .coachId(groupScheduleBatchCommand.coachId())
                .locationId(groupScheduleBatchCommand.locationId())
                .startDate(groupScheduleBatchCommand.startDate())
                .endDate(groupScheduleBatchCommand.endDate())
                .type(groupScheduleBatchCommand.type())
                .slots(groupScheduleBatchCommand.slots())
                .excludeScheduleIds(List.of())
                .build());

        List<GroupSchedule> schedules = groupScheduleBatchCommand.slots().stream()
                .map(slot -> GroupSchedule.builder()
                        .id(UUID.randomUUID())
                        .groupId(groupId)
                        .coachId(groupScheduleBatchCommand.coachId())
                        .locationId(groupScheduleBatchCommand.locationId())
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

    @Transactional(readOnly = true)
    public List<GroupScheduleDto> getSchedules(ScheduleSearchCriteria criteria) {
        return groupScheduleRepository.findAll(
                        GroupScheduleSpecification.byCriteria(criteria)
                ).stream()
                .map(GroupScheduleMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupScheduleDto> getActiveSchedulesByGroup(UUID groupId) {
        return getSchedules(
                ScheduleSearchCriteria.builder()
                        .groupId(groupId)
                        .status(ScheduleStatus.ACTIVE)
                        .build()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupScheduleDto> getActiveSchedulesByGroup(UUID groupId, LocalDate date) {
        return getSchedules(
                ScheduleSearchCriteria.builder()
                        .groupId(groupId)
                        .fromDate(date)
                        .toDate(date)
                        .dayOfWeek(date.getDayOfWeek())
                        .status(ScheduleStatus.ACTIVE)
                        .build()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupScheduleDto> getActiveSchedulesByCoach(UUID coachId) {
        return getSchedules(
                ScheduleSearchCriteria.builder()
                        .coachId(coachId)
                        .status(ScheduleStatus.ACTIVE)
                        .build()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupScheduleDto> getActiveSchedulesByCoach(UUID coachId, LocalDate date) {
        return getSchedules(
                ScheduleSearchCriteria.builder()
                        .coachId(coachId)
                        .fromDate(date)
                        .toDate(date)
                        .dayOfWeek(date.getDayOfWeek())
                        .status(ScheduleStatus.ACTIVE)
                        .build()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupScheduleDto> getActiveSchedulesByCoachAndDay(UUID coachId, DayOfWeek dayOfWeek) {
        return getSchedules(
                ScheduleSearchCriteria.builder()
                        .coachId(coachId)
                        .dayOfWeek(dayOfWeek)
                        .status(ScheduleStatus.ACTIVE)
                        .build()
        );
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
    public void activateSchedule(UUID scheduleId) {
        GroupSchedule groupSchedule = groupScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("Group schedule not found", scheduleId));

        if (groupSchedule.getStatus() == ScheduleStatus.ACTIVE) {
            return;
        }
        if (groupSchedule.getStatus() == ScheduleStatus.DELETED) {
            throw new BadRequestException("Schedule has been deleted");
        }

        groupSchedule.setStatus(ScheduleStatus.ACTIVE);
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
        ensureValid(groupId, GroupScheduleValidationCommand.builder()
                .coachId(command.coachId())
                .locationId(command.locationId())
                .startDate(command.startDate())
                .endDate(command.endDate())
                .type(command.type())
                .slots(command.slots())
                .excludeScheduleIds(ignoreIds)
                .build());

        // 4. закрываем СТАРЫЙ batch
        currentBatch.forEach(s -> s.setStatus(ScheduleStatus.DELETED));

        // 5. создаём НОВЫЙ batch
        List<GroupSchedule> newBatch = command.slots().stream()
                .map(slot -> GroupSchedule.builder()
                        .id(UUID.randomUUID())
                        .groupId(groupId)
                        .coachId(command.coachId())
                        .locationId(command.locationId())
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

    private void ensureValid(UUID groupId, GroupScheduleValidationCommand command) {
        ScheduleValidationResult result = groupScheduleValidationService.validate(groupId, command);
        if (!result.valid()) {
            throw new BadRequestException(
                    result.conflicts().getFirst().message(),
                    result.conflicts()
            );
        }
    }
}
