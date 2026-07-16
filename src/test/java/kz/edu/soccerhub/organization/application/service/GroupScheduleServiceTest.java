package kz.edu.soccerhub.organization.application.service;

import kz.edu.soccerhub.common.dto.group.DayScheduleSlot;
import kz.edu.soccerhub.common.dto.group.ScheduleValidationResult;
import kz.edu.soccerhub.common.dto.group.UpdateScheduleBatchCommand;
import kz.edu.soccerhub.common.port.TrainingSessionPlanningPort;
import kz.edu.soccerhub.organization.domain.model.GroupSchedule;
import kz.edu.soccerhub.organization.domain.model.enums.ScheduleStatus;
import kz.edu.soccerhub.organization.domain.model.enums.ScheduleType;
import kz.edu.soccerhub.organization.domain.repository.GroupScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupScheduleServiceTest {

    @Mock
    private GroupScheduleRepository groupScheduleRepository;
    @Mock
    private GroupScheduleValidationService groupScheduleValidationService;
    @Mock
    private TrainingSessionPlanningPort trainingSessionPlanningPort;

    private GroupScheduleService service;

    @BeforeEach
    void setUp() {
        service = new GroupScheduleService(
                groupScheduleRepository,
                groupScheduleValidationService,
                trainingSessionPlanningPort
        );
        when(groupScheduleValidationService.validate(any(), any()))
                .thenReturn(ScheduleValidationResult.of(List.of()));
    }

    @Test
    void shouldPreserveUnchangedSlotsAndResyncOnlyAddedAndRemovedSlots() {
        UUID groupId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        GroupSchedule monday = schedule(groupId, coachId, locationId, DayOfWeek.MONDAY, startDate, endDate);
        GroupSchedule wednesday = schedule(groupId, coachId, locationId, DayOfWeek.WEDNESDAY, startDate, endDate);

        when(groupScheduleRepository.findBatch(
                groupId, coachId, ScheduleType.REGULAR, startDate, endDate, ScheduleStatus.ACTIVE
        )).thenReturn(List.of(monday, wednesday));

        service.updateScheduleBatch(groupId, command(
                coachId,
                locationId,
                startDate,
                endDate,
                List.of(slot(DayOfWeek.MONDAY), slot(DayOfWeek.FRIDAY))
        ));

        assertEquals(ScheduleStatus.ACTIVE, monday.getStatus());
        assertEquals(ScheduleStatus.DELETED, wednesday.getStatus());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GroupSchedule>> createdCaptor = ArgumentCaptor.forClass(List.class);
        verify(groupScheduleRepository).saveAll(createdCaptor.capture());
        List<GroupSchedule> created = createdCaptor.getValue();
        assertEquals(1, created.size());
        assertEquals(DayOfWeek.FRIDAY, created.getFirst().getDayOfWeek());

        InOrder planningOrder = inOrder(trainingSessionPlanningPort);
        planningOrder.verify(trainingSessionPlanningPort).cancelFuturePlannedSessions(
                eq(List.of(wednesday.getId())),
                any(LocalDate.class),
                eq("Schedule changed")
        );
        planningOrder.verify(trainingSessionPlanningPort).materializeSchedules(List.of(created.getFirst().getId()));
        verify(trainingSessionPlanningPort, never()).resyncSchedules(any(), any(), any());
    }

    @Test
    void shouldLeaveSessionsUntouchedWhenSlotsDidNotChange() {
        UUID groupId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        GroupSchedule monday = schedule(groupId, coachId, locationId, DayOfWeek.MONDAY, startDate, endDate);

        when(groupScheduleRepository.findBatch(
                groupId, coachId, ScheduleType.REGULAR, startDate, endDate, ScheduleStatus.ACTIVE
        )).thenReturn(List.of(monday));

        service.updateScheduleBatch(groupId, command(
                coachId,
                locationId,
                startDate,
                endDate,
                List.of(slot(DayOfWeek.MONDAY))
        ));

        assertSame(ScheduleStatus.ACTIVE, monday.getStatus());
        verify(groupScheduleRepository, never()).saveAll(any());
        verify(trainingSessionPlanningPort, never()).materializeSchedules(any());
        verify(trainingSessionPlanningPort, never()).cancelFuturePlannedSessions(any(), any(), any());
        verify(trainingSessionPlanningPort, never()).resyncSchedules(any(), any(), any());
    }

    private UpdateScheduleBatchCommand command(
            UUID coachId,
            UUID locationId,
            LocalDate startDate,
            LocalDate endDate,
            List<DayScheduleSlot> slots
    ) {
        return UpdateScheduleBatchCommand.builder()
                .coachId(coachId)
                .locationId(locationId)
                .startDate(startDate)
                .endDate(endDate)
                .type(ScheduleType.REGULAR)
                .slots(slots)
                .build();
    }

    private GroupSchedule schedule(
            UUID groupId,
            UUID coachId,
            UUID locationId,
            DayOfWeek dayOfWeek,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return GroupSchedule.builder()
                .id(UUID.randomUUID())
                .groupId(groupId)
                .coachId(coachId)
                .locationId(locationId)
                .dayOfWeek(dayOfWeek)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .startDate(startDate)
                .endDate(endDate)
                .scheduleType(ScheduleType.REGULAR)
                .status(ScheduleStatus.ACTIVE)
                .build();
    }

    private DayScheduleSlot slot(DayOfWeek dayOfWeek) {
        return DayScheduleSlot.builder()
                .dayOfWeek(dayOfWeek)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build();
    }
}
