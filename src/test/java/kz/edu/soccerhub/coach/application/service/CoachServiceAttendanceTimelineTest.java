package kz.edu.soccerhub.coach.application.service;

import kz.edu.soccerhub.auth.domain.repository.AppUserRepo;
import kz.edu.soccerhub.coach.domain.model.TrainingSession;
import kz.edu.soccerhub.coach.domain.model.TrainingSessionAttendance;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionAttendanceStatus;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;
import kz.edu.soccerhub.coach.domain.repository.CoachAvailabilityRepository;
import kz.edu.soccerhub.coach.domain.repository.CoachBranchRepository;
import kz.edu.soccerhub.coach.domain.repository.CoachProfileRepository;
import kz.edu.soccerhub.coach.domain.repository.CoachStatusHistoryRepository;
import kz.edu.soccerhub.coach.domain.repository.TrainingSessionAttendanceRepository;
import kz.edu.soccerhub.coach.domain.repository.TrainingSessionRepository;
import kz.edu.soccerhub.common.dto.coach.PlayerAttendanceTimelineRecordDto;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.organization.domain.repository.GroupScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoachServiceAttendanceTimelineTest {

    @Mock
    private CoachProfileRepository coachProfileRepository;
    @Mock
    private CoachBranchService coachBranchService;
    @Mock
    private CoachBranchRepository coachBranchRepository;
    @Mock
    private TrainingSessionRepository trainingSessionRepository;
    @Mock
    private TrainingSessionAttendanceRepository trainingSessionAttendanceRepository;
    @Mock
    private CoachAvailabilityRepository coachAvailabilityRepository;
    @Mock
    private CoachStatusHistoryRepository coachStatusHistoryRepository;
    @Mock
    private AppUserRepo appUserRepo;
    @Mock
    private GroupPort groupPort;
    @Mock
    private GroupScheduleRepository groupScheduleRepository;

    @InjectMocks
    private CoachService service;

    @Test
    void getAttendanceTimelineShouldExcludeCancelledAndFutureSessionsAndPreserveUnmarkedRows() {
        UUID playerId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(30);
        LocalDate to = today.plusDays(1);
        TrainingSession marked = session(groupId, today.minusDays(2), TrainingSessionStatus.COMPLETED, 18);
        TrainingSession unmarked = session(groupId, today.minusDays(1), TrainingSessionStatus.PLANNED, 18);
        TrainingSession cancelled = session(groupId, today.minusDays(3), TrainingSessionStatus.CANCELLED, 18);
        TrainingSession future = session(groupId, today.plusDays(1), TrainingSessionStatus.PLANNED, 18);
        TrainingSessionAttendance attendance = TrainingSessionAttendance.builder()
                .id(UUID.randomUUID())
                .sessionId(marked.getId())
                .playerId(playerId)
                .status(TrainingSessionAttendanceStatus.PRESENT)
                .comment("On time")
                .markedAt(LocalDateTime.now())
                .build();

        when(trainingSessionRepository.findByGroupIdInAndSessionDateBetweenOrderBySessionDateDescScheduledStartAtDesc(
                Set.of(groupId), from, to
        )).thenReturn(List.of(future, unmarked, marked, cancelled));
        when(trainingSessionAttendanceRepository.findBySessionIdInAndPlayerId(
                Set.of(unmarked.getId(), marked.getId()), playerId
        )).thenReturn(List.of(attendance));

        List<PlayerAttendanceTimelineRecordDto> result = service.getAttendanceTimeline(
                playerId, Set.of(groupId), from, to
        );

        assertEquals(2, result.size());
        assertEquals(unmarked.getId(), result.getFirst().sessionId());
        assertEquals("OVERDUE", result.getFirst().effectiveSessionStatus());
        assertNull(result.getFirst().attendanceStatus());
        assertEquals(TrainingSessionAttendanceStatus.PRESENT, result.get(1).attendanceStatus());
        assertEquals("On time", result.get(1).comment());
    }

    private TrainingSession session(UUID groupId, LocalDate date, TrainingSessionStatus status, int hour) {
        return TrainingSession.builder()
                .id(UUID.randomUUID())
                .groupId(groupId)
                .coachId(UUID.randomUUID())
                .sessionDate(date)
                .scheduledStartAt(date.atTime(hour, 0))
                .scheduledEndAt(date.atTime(hour + 1, 0))
                .status(status)
                .build();
    }
}
