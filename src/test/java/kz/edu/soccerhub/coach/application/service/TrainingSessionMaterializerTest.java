package kz.edu.soccerhub.coach.application.service;

import kz.edu.soccerhub.coach.domain.model.TrainingSession;
import kz.edu.soccerhub.coach.domain.repository.TrainingSessionRepository;
import kz.edu.soccerhub.organization.domain.model.GroupSchedule;
import kz.edu.soccerhub.organization.domain.model.enums.ScheduleStatus;
import kz.edu.soccerhub.organization.domain.repository.GroupScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TrainingSessionMaterializerTest {
    private final GroupScheduleRepository schedules = mock(GroupScheduleRepository.class);
    private final TrainingSessionRepository sessions = mock(TrainingSessionRepository.class);
    private final TrainingSessionMaterializer service = new TrainingSessionMaterializer(schedules, sessions);
    private GroupSchedule schedule;
    private final LocalDate future = LocalDate.now(ZoneId.of("Asia/Almaty")).plusDays(180);

    @BeforeEach
    void setUp() {
        schedule = GroupSchedule.builder().id(UUID.randomUUID()).groupId(UUID.randomUUID()).coachId(UUID.randomUUID())
                .startDate(future).endDate(future).dayOfWeek(future.getDayOfWeek())
                .startTime(LocalTime.of(18,0)).endTime(LocalTime.of(19,0)).status(ScheduleStatus.ACTIVE).build();
        when(schedules.findAllById(Set.of(schedule.getId()))).thenReturn(List.of(schedule));
    }

    @Test
    void explicitSingleDateCreatesExactlyOneSessionBeyondRollingHorizon() {
        service.materializeSchedules(List.of(schedule.getId()));
        ArgumentCaptor<TrainingSession> captured = ArgumentCaptor.forClass(TrainingSession.class);
        verify(sessions).save(captured.capture());
        assertEquals(future, captured.getValue().getSessionDate());
        assertEquals(future.atTime(18,0), captured.getValue().getScheduledStartAt());
        assertEquals(schedule.getGroupId(), captured.getValue().getGroupId());
    }

    @Test
    void recurringPeriodsStillRespectRollingHorizon() {
        schedule.setEndDate(future.plusMonths(2));
        service.materializeSchedules(List.of(schedule.getId()));
        verifyNoInteractions(sessions);
    }

    @Test
    void repeatedMaterializationDoesNotDuplicateTheSession() {
        when(sessions.findByScheduleIdAndSessionDate(schedule.getId(), future)).thenReturn(Optional.of(new TrainingSession()));
        service.materializeSchedules(List.of(schedule.getId()));
        verify(sessions, never()).save(any());
    }

    @Test
    void cancelledPeriodsDoNotCreateSessions() {
        schedule.setStatus(ScheduleStatus.CANCELLED);
        service.materializeSchedules(List.of(schedule.getId()));
        verifyNoInteractions(sessions);
    }
}
