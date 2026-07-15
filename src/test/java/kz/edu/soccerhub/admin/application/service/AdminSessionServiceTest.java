package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.session.*;
import kz.edu.soccerhub.coach.application.service.CoachRosterReader;
import kz.edu.soccerhub.coach.domain.model.TrainingSession;
import kz.edu.soccerhub.coach.domain.model.TrainingSessionAttendance;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionAttendanceStatus;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;
import kz.edu.soccerhub.coach.domain.repository.TrainingSessionAttendanceRepository;
import kz.edu.soccerhub.coach.domain.repository.TrainingSessionRepository;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.dto.group.GroupCoachDto;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.GroupActivityPort;
import kz.edu.soccerhub.common.port.GroupCoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.MediaAccessPort;
import kz.edu.soccerhub.common.port.MediaAvatarPort;
import kz.edu.soccerhub.organization.domain.model.Location;
import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import kz.edu.soccerhub.organization.domain.repository.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSessionServiceTest {

    @Mock
    private AdminService adminService;
    @Mock
    private AdminBranchService adminBranchService;
    @Mock
    private GroupPort groupPort;
    @Mock
    private GroupCoachPort groupCoachPort;
    @Mock
    private CoachPort coachPort;
    @Mock
    private TrainingSessionRepository trainingSessionRepository;
    @Mock
    private TrainingSessionAttendanceRepository trainingSessionAttendanceRepository;
    @Mock
    private CoachRosterReader coachRosterReader;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private GroupActivityPort groupActivityPort;
    @Mock
    private MediaAvatarPort mediaAvatarPort;
    @Mock
    private MediaAccessPort mediaAccessPort;

    private AdminSessionService service;

    @BeforeEach
    void setUp() {
        service = new AdminSessionService(
                adminService,
                adminBranchService,
                groupPort,
                groupCoachPort,
                coachPort,
                trainingSessionRepository,
                trainingSessionAttendanceRepository,
                coachRosterReader,
                locationRepository,
                groupActivityPort,
                mediaAvatarPort,
                mediaAccessPort
        );
    }

    @Test
    void shouldReturnGroupSessionsForDateRange() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now().plusDays(7);
        LocalDate sessionDate = LocalDate.now().plusDays(1);

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(groupPort.getGroupById(groupId)).thenReturn(GroupDto.builder()
                .groupId(groupId)
                .branchId(branchId)
                .name("Adal")
                .status(GroupStatus.ACTIVE)
                .build());
        when(trainingSessionRepository.findByGroupIdAndSessionDateBetweenOrderBySessionDateAscScheduledStartAtAsc(groupId, from, to))
                .thenReturn(List.of(TrainingSession.builder()
                        .id(sessionId)
                        .groupId(groupId)
                        .coachId(coachId)
                        .locationId(locationId)
                        .scheduleId(UUID.randomUUID())
                        .sessionDate(sessionDate)
                        .scheduledStartAt(LocalDateTime.of(sessionDate, java.time.LocalTime.of(20, 0)))
                        .scheduledEndAt(LocalDateTime.of(sessionDate, java.time.LocalTime.of(21, 0)))
                        .status(TrainingSessionStatus.PLANNED)
                        .reportDone(false)
                        .build()));
        when(groupCoachPort.getActiveCoaches(groupId)).thenReturn(List.of(
                GroupCoachDto.builder().groupId(groupId).coachId(coachId).role(CoachRole.MAIN).active(true).build()
        ));
        when(coachPort.getCoaches(Set.of(coachId))).thenReturn(List.of(
                CoachDto.builder().id(coachId).firstName("Арсен").lastName("Рахметулы").active(true).build()
        ));
        when(locationRepository.findAllById(Set.of(locationId))).thenReturn(List.of(
                Location.builder().id(locationId).name("Поле №2").build()
        ));
        when(trainingSessionAttendanceRepository.findBySessionIdIn(Set.of(sessionId))).thenReturn(List.of(
                TrainingSessionAttendance.builder()
                        .id(UUID.randomUUID())
                        .sessionId(sessionId)
                        .playerId(UUID.randomUUID())
                        .status(TrainingSessionAttendanceStatus.PRESENT)
                        .build()
        ));
        when(coachRosterReader.getActivePlayersByGroupAndDate(groupId, sessionDate)).thenReturn(List.of(
                new CoachRosterReader.ActivePlayerView(UUID.randomUUID(), "A", "B"),
                new CoachRosterReader.ActivePlayerView(UUID.randomUUID(), "C", "D")
        ));

        AdminGroupSessionsOutput output = service.getGroupSessions(adminId, groupId, from, to, null, null);

        assertEquals(groupId, output.groupId());
        assertEquals(1, output.items().size());
        AdminGroupSessionsOutput.SessionItem item = output.items().getFirst();
        assertEquals(sessionId, item.id());
        assertEquals("PLANNED", item.status());
        assertEquals("PLANNED", item.effectiveStatus());
        assertEquals(2, item.participantsCount());
        assertEquals(1, item.attendance().marked());
        assertEquals(1, item.attendance().presentLike());
        assertEquals("Арсен Рахметулы", item.coaches().getFirst().fullName());
        assertEquals("Поле №2", item.location().name());
        assertTrue(item.capabilities().canCancel());
        assertTrue(item.capabilities().canReschedule());
        assertTrue(item.capabilities().canOpenAttendance());
    }

    @Test
    void shouldReturnGroupAttendanceForDateRange() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        UUID sessionOneId = UUID.randomUUID();
        UUID sessionTwoId = UUID.randomUUID();
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now().plusDays(7);
        LocalDate sessionOneDate = LocalDate.now();
        LocalDate sessionTwoDate = LocalDate.now().plusDays(1);

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(groupPort.getGroupById(groupId)).thenReturn(GroupDto.builder()
                .groupId(groupId)
                .branchId(branchId)
                .name("Adal")
                .status(GroupStatus.ACTIVE)
                .build());
        when(trainingSessionRepository.findByGroupIdAndSessionDateBetweenOrderBySessionDateAscScheduledStartAtAsc(groupId, from, to))
                .thenReturn(List.of(
                        TrainingSession.builder()
                                .id(sessionOneId)
                                .groupId(groupId)
                                .coachId(coachId)
                                .sessionDate(sessionOneDate)
                                .scheduledStartAt(LocalDateTime.of(sessionOneDate, java.time.LocalTime.of(20, 0)))
                                .scheduledEndAt(LocalDateTime.of(sessionOneDate, java.time.LocalTime.of(21, 0)))
                                .status(TrainingSessionStatus.IN_PROGRESS)
                                .reportDone(false)
                                .build(),
                        TrainingSession.builder()
                                .id(sessionTwoId)
                                .groupId(groupId)
                                .coachId(coachId)
                                .sessionDate(sessionTwoDate)
                                .scheduledStartAt(LocalDateTime.of(sessionTwoDate, java.time.LocalTime.of(20, 0)))
                                .scheduledEndAt(LocalDateTime.of(sessionTwoDate, java.time.LocalTime.of(21, 0)))
                                .status(TrainingSessionStatus.PLANNED)
                                .reportDone(false)
                                .build()
                ));
        when(trainingSessionAttendanceRepository.findBySessionIdIn(Set.of(sessionOneId, sessionTwoId))).thenReturn(List.of(
                TrainingSessionAttendance.builder()
                        .id(UUID.randomUUID())
                        .sessionId(sessionOneId)
                        .playerId(UUID.randomUUID())
                        .status(TrainingSessionAttendanceStatus.PRESENT)
                        .build(),
                TrainingSessionAttendance.builder()
                        .id(UUID.randomUUID())
                        .sessionId(sessionOneId)
                        .playerId(UUID.randomUUID())
                        .status(TrainingSessionAttendanceStatus.EXCUSED)
                        .build()
        ));
        when(coachRosterReader.getActivePlayersByGroupAndDate(groupId, sessionOneDate)).thenReturn(List.of(
                new CoachRosterReader.ActivePlayerView(UUID.randomUUID(), "A", "B"),
                new CoachRosterReader.ActivePlayerView(UUID.randomUUID(), "C", "D"),
                new CoachRosterReader.ActivePlayerView(UUID.randomUUID(), "E", "F")
        ));
        when(coachRosterReader.getActivePlayersByGroupAndDate(groupId, sessionTwoDate)).thenReturn(List.of(
                new CoachRosterReader.ActivePlayerView(UUID.randomUUID(), "G", "H"),
                new CoachRosterReader.ActivePlayerView(UUID.randomUUID(), "I", "J")
        ));

        AdminGroupAttendanceOutput output = service.getGroupAttendance(adminId, groupId, from, to);

        assertEquals(groupId, output.groupId());
        assertEquals(2, output.summary().sessionsCount());
        assertEquals(1, output.summary().recordedSessionsCount());
        assertEquals(5, output.summary().totalParticipants());
        assertEquals(2, output.summary().totalMarked());
        assertEquals(1, output.summary().totalPresent());
        assertEquals(1, output.summary().totalExcused());
        assertEquals(3, output.summary().totalUnmarked());
        assertEquals(1, output.summary().totalPresentLike());
        assertEquals(20, output.summary().averageAttendanceRate());
        assertEquals(2, output.sessions().size());
        assertTrue(output.sessions().getFirst().capabilities().canOpenAttendance());
        assertTrue(output.sessions().getFirst().capabilities().canEditAttendance());
        assertTrue(output.sessions().get(1).capabilities().canOpenAttendance());
        assertFalse(output.sessions().get(1).capabilities().canEditAttendance());
    }

    @Test
    void shouldReturnSessionDetailsWithOverdueStatus() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        LocalDate sessionDate = LocalDate.now().minusDays(1);

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(trainingSessionRepository.findById(sessionId)).thenReturn(Optional.of(TrainingSession.builder()
                .id(sessionId)
                .groupId(groupId)
                .coachId(coachId)
                .locationId(locationId)
                .sessionDate(sessionDate)
                .scheduledStartAt(LocalDateTime.of(sessionDate, java.time.LocalTime.of(20, 0)))
                .scheduledEndAt(LocalDateTime.of(sessionDate, java.time.LocalTime.of(21, 0)))
                .status(TrainingSessionStatus.PLANNED)
                .reportDone(false)
                .build()));
        when(groupPort.getGroupById(groupId)).thenReturn(GroupDto.builder()
                .groupId(groupId)
                .branchId(branchId)
                .name("Adal")
                .status(GroupStatus.ACTIVE)
                .build());
        when(groupCoachPort.getActiveCoaches(groupId)).thenReturn(List.of(
                GroupCoachDto.builder().groupId(groupId).coachId(coachId).role(CoachRole.ASSISTANT).active(true).build()
        ));
        when(coachPort.getCoaches(Set.of(coachId))).thenReturn(List.of(
                CoachDto.builder().id(coachId).firstName("Арсен").lastName("Гизатов").active(true).build()
        ));
        when(locationRepository.findAllById(Set.of(locationId))).thenReturn(List.of(
                Location.builder().id(locationId).name("Поле №1").build()
        ));
        when(coachRosterReader.getActivePlayersByGroupAndDate(groupId, sessionDate)).thenReturn(List.of(
                new CoachRosterReader.ActivePlayerView(UUID.randomUUID(), "A", "B"),
                new CoachRosterReader.ActivePlayerView(UUID.randomUUID(), "C", "D"),
                new CoachRosterReader.ActivePlayerView(UUID.randomUUID(), "E", "F")
        ));
        when(trainingSessionAttendanceRepository.findBySessionId(sessionId)).thenReturn(List.of(
                TrainingSessionAttendance.builder()
                        .id(UUID.randomUUID())
                        .sessionId(sessionId)
                        .playerId(UUID.randomUUID())
                        .status(TrainingSessionAttendanceStatus.PRESENT)
                        .build(),
                TrainingSessionAttendance.builder()
                        .id(UUID.randomUUID())
                        .sessionId(sessionId)
                        .playerId(UUID.randomUUID())
                        .status(TrainingSessionAttendanceStatus.ABSENT)
                        .build()
        ));

        AdminSessionDetailsOutput output = service.getSessionDetails(adminId, sessionId);

        assertEquals("PLANNED", output.status());
        assertEquals("OVERDUE", output.effectiveStatus());
        assertEquals(3, output.participantsCount());
        assertEquals(2, output.attendance().marked());
        assertEquals(1, output.attendance().presentLike());
        assertEquals("ASSISTANT", output.coaches().getFirst().role());
        assertEquals("Поле №1", output.location().name());
        assertFalse(output.capabilities().canCancel());
        assertFalse(output.capabilities().canReschedule());
        assertTrue(output.capabilities().canOpenAttendance());
    }

    @Test
    void shouldAllowOpeningAttendanceForCompletedSessionButKeepReadOnly() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        LocalDate sessionDate = LocalDate.now().minusDays(2);

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(trainingSessionRepository.findById(sessionId)).thenReturn(Optional.of(TrainingSession.builder()
                .id(sessionId)
                .groupId(groupId)
                .coachId(coachId)
                .sessionDate(sessionDate)
                .scheduledStartAt(LocalDateTime.of(sessionDate, java.time.LocalTime.of(20, 0)))
                .scheduledEndAt(LocalDateTime.of(sessionDate, java.time.LocalTime.of(21, 0)))
                .status(TrainingSessionStatus.COMPLETED)
                .reportDone(true)
                .build()));
        when(groupPort.getGroupById(groupId)).thenReturn(GroupDto.builder()
                .groupId(groupId)
                .branchId(branchId)
                .name("Adal")
                .status(GroupStatus.ACTIVE)
                .build());
        when(groupCoachPort.getActiveCoaches(groupId)).thenReturn(List.of(
                GroupCoachDto.builder().groupId(groupId).coachId(coachId).role(CoachRole.MAIN).active(true).build()
        ));
        when(coachPort.getCoaches(Set.of(coachId))).thenReturn(List.of(
                CoachDto.builder().id(coachId).firstName("Арсен").lastName("Рахметулы").active(true).build()
        ));
        when(coachRosterReader.getActivePlayersByGroupAndDate(groupId, sessionDate)).thenReturn(List.of(
                new CoachRosterReader.ActivePlayerView(UUID.randomUUID(), "A", "B")
        ));
        when(trainingSessionAttendanceRepository.findBySessionId(sessionId)).thenReturn(List.of(
                TrainingSessionAttendance.builder()
                        .id(UUID.randomUUID())
                        .sessionId(sessionId)
                        .playerId(UUID.randomUUID())
                        .status(TrainingSessionAttendanceStatus.PRESENT)
                        .build()
        ));

        AdminSessionDetailsOutput details = service.getSessionDetails(adminId, sessionId);
        AdminSessionAttendanceOutput attendance = service.getSessionAttendance(adminId, sessionId);

        assertTrue(details.capabilities().canOpenAttendance());
        assertFalse(attendance.capabilities().canEdit());
    }

    @Test
    void shouldReturnSessionAttendanceForActiveRoster() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        UUID playerPresent = UUID.randomUUID();
        UUID playerLate = UUID.randomUUID();
        UUID playerUnmarked = UUID.randomUUID();
        LocalDate sessionDate = LocalDate.now();

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(trainingSessionRepository.findById(sessionId)).thenReturn(Optional.of(TrainingSession.builder()
                .id(sessionId)
                .groupId(groupId)
                .coachId(coachId)
                .sessionDate(sessionDate)
                .scheduledStartAt(LocalDateTime.of(sessionDate, java.time.LocalTime.of(20, 0)))
                .scheduledEndAt(LocalDateTime.of(sessionDate, java.time.LocalTime.of(21, 0)))
                .status(TrainingSessionStatus.IN_PROGRESS)
                .reportDone(false)
                .build()));
        when(groupPort.getGroupById(groupId)).thenReturn(GroupDto.builder()
                .groupId(groupId)
                .branchId(branchId)
                .name("Adal")
                .status(GroupStatus.ACTIVE)
                .build());
        when(coachRosterReader.getActivePlayersByGroupAndDate(groupId, sessionDate)).thenReturn(List.of(
                new CoachRosterReader.ActivePlayerView(playerPresent, "Alihan", "Serikov"),
                new CoachRosterReader.ActivePlayerView(playerLate, "Miras", "Akhmetov"),
                new CoachRosterReader.ActivePlayerView(playerUnmarked, "Dias", "Nurlybek")
        ));
        when(trainingSessionAttendanceRepository.findBySessionId(sessionId)).thenReturn(List.of(
                TrainingSessionAttendance.builder()
                        .id(UUID.randomUUID())
                        .sessionId(sessionId)
                        .playerId(playerPresent)
                        .status(TrainingSessionAttendanceStatus.PRESENT)
                        .comment(null)
                        .build(),
                TrainingSessionAttendance.builder()
                        .id(UUID.randomUUID())
                        .sessionId(sessionId)
                        .playerId(playerLate)
                        .status(TrainingSessionAttendanceStatus.LATE)
                        .comment("10 минут")
                        .build()
        ));

        AdminSessionAttendanceOutput output = service.getSessionAttendance(adminId, sessionId);

        assertEquals(sessionId, output.sessionId());
        assertEquals(groupId, output.group().id());
        assertEquals(3, output.summary().total());
        assertEquals(2, output.summary().marked());
        assertEquals(1, output.summary().present());
        assertEquals(1, output.summary().late());
        assertEquals(0, output.summary().absent());
        assertEquals(0, output.summary().excused());
        assertEquals(1, output.summary().unmarked());
        assertEquals(2, output.summary().presentLike());
        assertTrue(output.capabilities().canEdit());
        assertEquals(3, output.participants().size());
        assertNull(output.participants().stream()
                .filter(item -> item.playerId().equals(playerUnmarked))
                .findFirst()
                .orElseThrow()
                .status());
    }

    @Test
    void shouldUpdateSessionAttendanceForManagedSession() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        LocalDate sessionDate = LocalDate.now();

        TrainingSession session = TrainingSession.builder()
                .id(sessionId)
                .groupId(groupId)
                .coachId(coachId)
                .sessionDate(sessionDate)
                .scheduledStartAt(LocalDateTime.of(sessionDate, java.time.LocalTime.of(20, 0)))
                .scheduledEndAt(LocalDateTime.now().minusMinutes(5))
                .status(TrainingSessionStatus.PLANNED)
                .reportDone(false)
                .build();

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(trainingSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(groupPort.getGroupById(groupId)).thenReturn(GroupDto.builder()
                .groupId(groupId)
                .branchId(branchId)
                .name("Adal")
                .status(GroupStatus.ACTIVE)
                .build());
        when(coachRosterReader.getActivePlayersByGroupAndDate(groupId, sessionDate)).thenReturn(List.of(
                new CoachRosterReader.ActivePlayerView(playerId, "Alihan", "Serikov")
        ));
        when(trainingSessionAttendanceRepository.findBySessionId(sessionId))
                .thenReturn(List.of())
                .thenReturn(List.of(
                        TrainingSessionAttendance.builder()
                                .id(UUID.randomUUID())
                                .sessionId(sessionId)
                                .playerId(playerId)
                                .status(TrainingSessionAttendanceStatus.EXCUSED)
                                .comment("Болел")
                                .build()
                ));
        when(trainingSessionAttendanceRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        AdminSessionAttendanceOutput output = service.updateSessionAttendance(
                adminId,
                sessionId,
                new AdminSessionAttendanceUpdateInput(List.of(
                        new AdminSessionAttendanceUpdateInput.Entry(playerId, TrainingSessionAttendanceStatus.EXCUSED, "Болел")
                ))
        );

        assertEquals(1, output.summary().marked());
        assertEquals(1, output.summary().excused());
        assertEquals(0, output.summary().unmarked());
        assertEquals(TrainingSessionAttendanceStatus.EXCUSED, output.participants().getFirst().status());
        assertEquals("Болел", output.participants().getFirst().comment());
    }

    @Test
    void shouldCancelSessionAndReturnUpdatedDetails() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        LocalDate sessionDate = LocalDate.now().plusDays(1);

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(trainingSessionRepository.findById(sessionId)).thenReturn(Optional.of(TrainingSession.builder()
                .id(sessionId)
                .groupId(groupId)
                .coachId(coachId)
                .sessionDate(sessionDate)
                .scheduledStartAt(LocalDateTime.of(sessionDate, java.time.LocalTime.of(20, 0)))
                .scheduledEndAt(LocalDateTime.of(sessionDate, java.time.LocalTime.of(21, 0)))
                .status(TrainingSessionStatus.PLANNED)
                .reportDone(false)
                .build()));
        when(groupPort.getGroupById(groupId)).thenReturn(GroupDto.builder()
                .groupId(groupId)
                .branchId(branchId)
                .name("Adal")
                .status(GroupStatus.ACTIVE)
                .build());
        when(groupCoachPort.getActiveCoaches(groupId)).thenReturn(List.of(
                GroupCoachDto.builder().groupId(groupId).coachId(coachId).role(CoachRole.MAIN).active(true).build()
        ));
        when(coachPort.getCoaches(Set.of(coachId))).thenReturn(List.of(
                CoachDto.builder().id(coachId).firstName("Арсен").lastName("Рахметулы").active(true).build()
        ));
        when(coachRosterReader.getActivePlayersByGroupAndDate(groupId, sessionDate)).thenReturn(List.of());
        when(trainingSessionAttendanceRepository.findBySessionId(sessionId)).thenReturn(List.of());

        AdminSessionDetailsOutput output = service.cancelSession(
                adminId,
                sessionId,
                new AdminCancelSessionInput("COACH_UNAVAILABLE", "Заболел")
        );

        assertEquals("CANCELLED", output.status());
        assertEquals("CANCELLED", output.effectiveStatus());
        assertEquals("COACH_UNAVAILABLE: Заболел", output.cancelReason());
        assertFalse(output.capabilities().canCancel());
    }

    @Test
    void shouldRescheduleSessionAndReturnUpdatedDetails() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID oldLocationId = UUID.randomUUID();
        UUID newLocationId = UUID.randomUUID();
        LocalDate sessionDate = LocalDate.now().plusDays(1);
        LocalDateTime newStart = LocalDateTime.of(sessionDate.plusDays(1), java.time.LocalTime.of(19, 0));
        LocalDateTime newEnd = LocalDateTime.of(sessionDate.plusDays(1), java.time.LocalTime.of(20, 0));

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(trainingSessionRepository.findById(sessionId)).thenReturn(Optional.of(TrainingSession.builder()
                .id(sessionId)
                .groupId(groupId)
                .coachId(coachId)
                .locationId(oldLocationId)
                .sessionDate(sessionDate)
                .scheduledStartAt(LocalDateTime.of(sessionDate, java.time.LocalTime.of(20, 0)))
                .scheduledEndAt(LocalDateTime.of(sessionDate, java.time.LocalTime.of(21, 0)))
                .status(TrainingSessionStatus.PLANNED)
                .reportDone(false)
                .build()));
        when(groupPort.getGroupById(groupId)).thenReturn(GroupDto.builder()
                .groupId(groupId)
                .branchId(branchId)
                .name("Adal")
                .status(GroupStatus.ACTIVE)
                .build());
        when(groupCoachPort.getActiveCoaches(groupId)).thenReturn(List.of(
                GroupCoachDto.builder().groupId(groupId).coachId(coachId).role(CoachRole.MAIN).active(true).build()
        ));
        when(coachPort.getCoaches(Set.of(coachId))).thenReturn(List.of(
                CoachDto.builder().id(coachId).firstName("Арсен").lastName("Рахметулы").active(true).build()
        ));
        when(coachRosterReader.getActivePlayersByGroupAndDate(groupId, newStart.toLocalDate())).thenReturn(List.of());
        when(trainingSessionAttendanceRepository.findBySessionId(sessionId)).thenReturn(List.of());
        when(locationRepository.findAllById(Set.of(newLocationId))).thenReturn(List.of(
                Location.builder().id(newLocationId).name("Поле №3").build()
        ));
        when(trainingSessionRepository.existsCoachConflict(coachId, newStart.toLocalDate(), newStart, newEnd, sessionId)).thenReturn(false);
        when(trainingSessionRepository.existsLocationConflict(newLocationId, newStart.toLocalDate(), newStart, newEnd, sessionId)).thenReturn(false);

        AdminSessionDetailsOutput output = service.rescheduleSession(
                adminId,
                sessionId,
                new AdminRescheduleSessionInput(newStart, newEnd, newLocationId, "Поле занято")
        );

        assertEquals(newStart, output.startsAt());
        assertEquals(newEnd, output.endsAt());
        assertEquals(newStart.toLocalDate(), output.sessionDate());
        assertEquals("Поле №3", output.location().name());
        assertTrue(output.capabilities().canCancel());
    }

    @Test
    void shouldSubstituteCoachAndReturnUpdatedDetails() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID currentCoachId = UUID.randomUUID();
        UUID substituteCoachId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        LocalDate sessionDate = LocalDate.now().plusDays(1);

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(trainingSessionRepository.findById(sessionId)).thenReturn(Optional.of(TrainingSession.builder()
                .id(sessionId)
                .groupId(groupId)
                .coachId(currentCoachId)
                .sessionDate(sessionDate)
                .scheduledStartAt(LocalDateTime.of(sessionDate, java.time.LocalTime.of(20, 0)))
                .scheduledEndAt(LocalDateTime.of(sessionDate, java.time.LocalTime.of(21, 0)))
                .status(TrainingSessionStatus.PLANNED)
                .reportDone(false)
                .build()));
        when(groupPort.getGroupById(groupId)).thenReturn(GroupDto.builder()
                .groupId(groupId)
                .branchId(branchId)
                .name("Adal")
                .status(GroupStatus.ACTIVE)
                .build());
        when(groupCoachPort.getActiveCoaches(groupId)).thenReturn(List.of(
                GroupCoachDto.builder().groupId(groupId).coachId(currentCoachId).role(CoachRole.MAIN).active(true).build(),
                GroupCoachDto.builder().groupId(groupId).coachId(substituteCoachId).role(CoachRole.ASSISTANT).active(true).build()
        ));
        when(coachPort.verifyCoach(substituteCoachId)).thenReturn(true);
        when(coachPort.getCoaches(Set.of(substituteCoachId))).thenReturn(List.of(
                CoachDto.builder().id(substituteCoachId).firstName("Арсен").lastName("Гизатов").active(true).build()
        ));
        when(coachRosterReader.getActivePlayersByGroupAndDate(groupId, sessionDate)).thenReturn(List.of());
        when(trainingSessionAttendanceRepository.findBySessionId(sessionId)).thenReturn(List.of());
        when(trainingSessionRepository.existsCoachConflict(substituteCoachId, sessionDate, LocalDateTime.of(sessionDate, java.time.LocalTime.of(20, 0)), LocalDateTime.of(sessionDate, java.time.LocalTime.of(21, 0)), sessionId))
                .thenReturn(false);

        AdminSessionDetailsOutput output = service.substituteCoach(
                adminId,
                sessionId,
                new AdminSubstituteCoachInput(currentCoachId, substituteCoachId, "Основной отсутствует")
        );

        assertEquals(substituteCoachId, output.coaches().getFirst().id());
        assertEquals("Арсен Гизатов", output.coaches().getFirst().fullName());
        assertEquals("ASSISTANT", output.coaches().getFirst().role());
    }
}
