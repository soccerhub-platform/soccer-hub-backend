package kz.edu.soccerhub.organization.application.service;

import kz.edu.soccerhub.common.dto.coach.CoachWorkingAvailability;
import kz.edu.soccerhub.common.dto.group.DayScheduleSlot;
import kz.edu.soccerhub.common.dto.group.GroupScheduleValidationCommand;
import kz.edu.soccerhub.common.dto.group.ScheduleValidationConflictCode;
import kz.edu.soccerhub.common.dto.group.ScheduleValidationResult;
import kz.edu.soccerhub.common.port.CoachWorkingAvailabilityPort;
import kz.edu.soccerhub.organization.domain.model.enums.ScheduleType;
import kz.edu.soccerhub.organization.domain.repository.GroupScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupScheduleValidationServiceTest {

    @Mock
    private GroupScheduleRepository groupScheduleRepository;
    @Mock
    private CoachWorkingAvailabilityPort coachWorkingAvailabilityPort;

    private GroupScheduleValidationService service;

    @BeforeEach
    void setUp() {
        service = new GroupScheduleValidationService(
                groupScheduleRepository,
                coachWorkingAvailabilityPort
        );
    }

    @Test
    void shouldAllowSlotInsideConfiguredWorkingAvailability() {
        UUID coachId = UUID.randomUUID();
        when(coachWorkingAvailabilityPort.findWorkingAvailability(coachId))
                .thenReturn(Optional.of(availability(DayOfWeek.MONDAY)));

        ScheduleValidationResult result = service.validate(
                UUID.randomUUID(),
                command(coachId, DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(12, 0))
        );

        assertTrue(result.valid());
    }

    @Test
    void shouldRejectDayOutsideConfiguredWorkingAvailability() {
        UUID coachId = UUID.randomUUID();
        when(coachWorkingAvailabilityPort.findWorkingAvailability(coachId))
                .thenReturn(Optional.of(availability(DayOfWeek.MONDAY)));

        ScheduleValidationResult result = service.validate(
                UUID.randomUUID(),
                command(coachId, DayOfWeek.SATURDAY, LocalTime.of(11, 0), LocalTime.of(12, 0))
        );

        assertFalse(result.valid());
        assertTrue(result.conflicts().stream().anyMatch(conflict ->
                conflict.code() == ScheduleValidationConflictCode.COACH_UNAVAILABLE_DAY
        ));
    }

    @Test
    void shouldRejectSlotOutsideConfiguredWorkingHours() {
        UUID coachId = UUID.randomUUID();
        when(coachWorkingAvailabilityPort.findWorkingAvailability(coachId))
                .thenReturn(Optional.of(availability(DayOfWeek.MONDAY)));

        ScheduleValidationResult result = service.validate(
                UUID.randomUUID(),
                command(coachId, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0))
        );

        assertFalse(result.valid());
        assertTrue(result.conflicts().stream().anyMatch(conflict ->
                conflict.code() == ScheduleValidationConflictCode.COACH_OUTSIDE_WORKING_HOURS
                        && LocalTime.of(10, 0).equals(conflict.overlapStart())
                        && LocalTime.of(20, 0).equals(conflict.overlapEnd())
        ));
    }

    @Test
    void shouldNotBlockScheduleWhenCoachHasNoConfiguredAvailability() {
        UUID coachId = UUID.randomUUID();
        when(coachWorkingAvailabilityPort.findWorkingAvailability(coachId))
                .thenReturn(Optional.empty());

        ScheduleValidationResult result = service.validate(
                UUID.randomUUID(),
                command(coachId, DayOfWeek.SATURDAY, LocalTime.of(20, 0), LocalTime.of(21, 0))
        );

        assertTrue(result.valid());
    }

    private CoachWorkingAvailability availability(DayOfWeek... days) {
        return new CoachWorkingAvailability(
                Set.of(days),
                LocalTime.of(10, 0),
                LocalTime.of(20, 0),
                "Asia/Almaty"
        );
    }

    private GroupScheduleValidationCommand command(
            UUID coachId,
            DayOfWeek day,
            LocalTime start,
            LocalTime end
    ) {
        return GroupScheduleValidationCommand.builder()
                .coachId(coachId)
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .type(ScheduleType.REGULAR)
                .slots(java.util.List.of(DayScheduleSlot.builder()
                        .dayOfWeek(day)
                        .startTime(start)
                        .endTime(end)
                        .build()))
                .build();
    }
}
