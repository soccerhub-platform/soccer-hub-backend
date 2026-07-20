package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.branch.AdminBranchesOutput;
import kz.edu.soccerhub.admin.application.dto.coach.AdminCoachUpdateInput;
import kz.edu.soccerhub.admin.application.dto.coach.AdminCreateCoachInput;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommandOutput;
import kz.edu.soccerhub.common.dto.coach.AdminCoachProfileOutput;
import kz.edu.soccerhub.common.dto.coach.AdminCoachOverviewOutput;
import kz.edu.soccerhub.common.dto.coach.CoachCreateCommand;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.dto.coach.CoachSessionAdminView;
import kz.edu.soccerhub.common.dto.coach.CoachStatusHistoryDto;
import kz.edu.soccerhub.common.dto.coach.CoachUpdateCommand;
import kz.edu.soccerhub.common.dto.client.GroupMemberDto;
import kz.edu.soccerhub.common.dto.group.GroupCoachDto;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.dto.group.GroupScheduleDto;
import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;
import kz.edu.soccerhub.common.port.AuthPort;
import kz.edu.soccerhub.common.port.BranchPort;
import kz.edu.soccerhub.common.port.ClientPort;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.GroupActivityPort;
import kz.edu.soccerhub.common.port.GroupCoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.GroupSchedulePort;
import kz.edu.soccerhub.common.port.MediaAccessPort;
import kz.edu.soccerhub.common.port.MediaAvatarPort;
import kz.edu.soccerhub.dispatcher.application.service.PasswordGenerator;
import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import kz.edu.soccerhub.organization.domain.repository.LocationRepository;
import kz.edu.soccerhub.media.domain.enums.MediaKind;
import kz.edu.soccerhub.media.domain.enums.MediaOwnerType;
import kz.edu.soccerhub.media.domain.model.MediaAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCoachServiceTest {

    @Mock
    private CoachPort coachPort;
    @Mock
    private AuthPort authPort;
    @Mock
    private AdminService adminService;
    @Mock
    private AdminBranchService adminBranchService;
    @Mock
    private PasswordGenerator passwordGenerator;
    @Mock
    private GroupSchedulePort groupSchedulePort;
    @Mock
    private GroupPort groupPort;
    @Mock
    private GroupCoachPort groupCoachPort;
    @Mock
    private ClientPort clientPort;
    @Mock
    private MediaAvatarPort mediaAvatarPort;
    @Mock
    private MediaAccessPort mediaAccessPort;
    @Mock
    private BranchPort branchPort;
    @Mock
    private GroupActivityPort groupActivityPort;
    @Mock
    private LocationRepository locationRepository;

    private AdminCoachService service;

    @BeforeEach
    void setUp() {
        service = new AdminCoachService(
                coachPort,
                authPort,
                adminService,
                adminBranchService,
                passwordGenerator,
                groupSchedulePort,
                groupPort,
                groupCoachPort,
                clientPort,
                mediaAvatarPort,
                mediaAccessPort,
                branchPort,
                groupActivityPort,
                locationRepository
        );
    }

    @Test
    void shouldPassBirthDateAndDescriptionWhenCreatingCoach() {
        UUID adminId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        LocalDate birthDate = LocalDate.of(1990, 4, 12);

        when(passwordGenerator.generate(6)).thenReturn("123456");
        when(authPort.register(any())).thenReturn(AuthRegisterCommandOutput.builder()
                .id(coachId)
                .email("coach@example.com")
                .build());
        when(coachPort.create(any())).thenReturn(coachId);

        service.createCoach(adminId, AdminCreateCoachInput.builder()
                .firstName("Aibek")
                .lastName("Coach")
                .email("coach@example.com")
                .birthDate(birthDate)
                .phone("+77010000000")
                .description("UEFA B licensed coach")
                .build());

        ArgumentCaptor<CoachCreateCommand> commandCaptor = ArgumentCaptor.forClass(CoachCreateCommand.class);
        verify(coachPort).create(commandCaptor.capture());
        CoachCreateCommand command = commandCaptor.getValue();
        assertEquals(coachId, command.id());
        assertEquals(birthDate, command.birthDate());
        assertEquals("UEFA B licensed coach", command.bio());
    }

    @Test
    void shouldPassBirthDateAndDescriptionWhenUpdatingCoach() {
        UUID adminId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        LocalDate birthDate = LocalDate.of(1988, 9, 20);

        when(adminService.findById(adminId)).thenReturn(java.util.Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.getAdminBranches(adminId)).thenReturn(List.of(
                AdminBranchesOutput.builder().branchId(branchId).name("Branch").build()
        ));
        when(coachPort.getBranchIds(coachId)).thenReturn(Set.of(branchId));

        service.updateCoach(adminId, coachId, new AdminCoachUpdateInput(
                "Aibek",
                "Coach",
                "coach@example.com",
                birthDate,
                "+77010000000",
                "U12",
                "Focuses on technical development"
        ));

        ArgumentCaptor<CoachUpdateCommand> commandCaptor = ArgumentCaptor.forClass(CoachUpdateCommand.class);
        verify(coachPort).update(commandCaptor.capture());
        CoachUpdateCommand command = commandCaptor.getValue();
        assertEquals(coachId, command.coachId());
        assertEquals(birthDate, command.birthDate());
        assertEquals("Focuses on technical development", command.bio());
    }

    @Test
    void shouldExtendCoachProfileGroupsReadModel() {
        UUID adminId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID firstGroupId = UUID.randomUUID();
        UUID secondGroupId = UUID.randomUUID();
        UUID firstGroupCoachId = UUID.randomUUID();
        UUID secondGroupCoachId = UUID.randomUUID();
        UUID firstSessionId = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        when(adminBranchService.getAdminBranches(adminId)).thenReturn(List.of(
                AdminBranchesOutput.builder().branchId(branchId).name("Branch").build()
        ));
        when(coachPort.getCoach(coachId)).thenReturn(CoachDto.builder()
                .id(coachId)
                .firstName("Aibek")
                .lastName("Coach")
                .email("coach@example.com")
                .birthDate(LocalDate.of(1990, 4, 12))
                .phone("+77010000000")
                .specialization("U10")
                .bio("UEFA B licensed coach")
                .active(true)
                .build());
        when(coachPort.getBranchIds(coachId)).thenReturn(Set.of(branchId));
        when(groupCoachPort.getActiveAssignmentsByCoachId(coachId)).thenReturn(List.of(
                GroupCoachDto.builder()
                        .id(firstGroupCoachId)
                        .groupId(firstGroupId)
                        .coachId(coachId)
                        .role(CoachRole.MAIN)
                        .active(true)
                        .build(),
                GroupCoachDto.builder()
                        .id(secondGroupCoachId)
                        .groupId(secondGroupId)
                        .coachId(coachId)
                        .role(null)
                        .active(true)
                        .build()
        ));
        UUID completedAssignmentId = UUID.randomUUID();
        when(groupCoachPort.getAssignmentsByCoachId(coachId)).thenReturn(List.of(
                GroupCoachDto.builder()
                        .id(completedAssignmentId)
                        .groupId(firstGroupId)
                        .coachId(coachId)
                        .role(CoachRole.ASSISTANT)
                        .active(false)
                        .assignedFrom(today.minusMonths(4))
                        .assignedTo(today.minusMonths(1))
                        .removalReason("Schedule changed")
                        .build()
        ));
        when(groupPort.getGroupsByIds(Set.of(firstGroupId, secondGroupId))).thenReturn(List.of(
                GroupDto.builder().groupId(firstGroupId).name("Falcons").branchId(branchId).status(GroupStatus.ACTIVE).build(),
                GroupDto.builder().groupId(secondGroupId).name("Wolves").branchId(branchId).status(GroupStatus.ACTIVE).build()
        ));
        when(groupSchedulePort.getActiveSchedulesByCoach(coachId)).thenReturn(List.of(
                GroupScheduleDto.builder()
                        .scheduleId(UUID.randomUUID())
                        .groupId(firstGroupId)
                        .coachId(coachId)
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .startTime(LocalTime.of(18, 0))
                        .endTime(LocalTime.of(19, 0))
                        .startDate(today.minusDays(7))
                        .endDate(today.plusDays(30))
                        .status("ACTIVE")
                        .build(),
                GroupScheduleDto.builder()
                        .scheduleId(UUID.randomUUID())
                        .groupId(secondGroupId)
                        .coachId(coachId)
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .startTime(LocalTime.of(18, 30))
                        .endTime(LocalTime.of(19, 30))
                        .startDate(today.minusDays(7))
                        .endDate(today.plusDays(30))
                        .status("ACTIVE")
                        .build(),
                GroupScheduleDto.builder()
                        .scheduleId(UUID.randomUUID())
                        .groupId(firstGroupId)
                        .coachId(coachId)
                        .dayOfWeek(DayOfWeek.WEDNESDAY)
                        .startTime(LocalTime.of(18, 0))
                        .endTime(LocalTime.of(19, 0))
                        .startDate(today.minusDays(7))
                        .endDate(today.plusDays(30))
                        .status("ACTIVE")
                        .build()
        ));
        when(coachPort.getSessions(
                Set.of(coachId),
                Set.of(firstGroupId, secondGroupId),
                today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                today.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        )).thenReturn(List.of(
                new CoachSessionAdminView(
                        firstSessionId,
                        coachId,
                        firstGroupId,
                        UUID.randomUUID(),
                        "REGULAR",
                        today.plusDays(1),
                        LocalDateTime.of(today.plusDays(1), LocalTime.of(18, 0)),
                        LocalDateTime.of(today.plusDays(1), LocalTime.of(19, 0)),
                        "PLANNED",
                        false,
                        null
                )
        ));
        when(coachPort.getUpcomingSessions(coachId, today)).thenReturn(List.of(
                new CoachSessionAdminView(
                        UUID.randomUUID(),
                        coachId,
                        firstGroupId,
                        UUID.randomUUID(),
                        "REGULAR",
                        today.plusDays(2),
                        LocalDateTime.of(today.plusDays(2), LocalTime.of(19, 0)),
                        LocalDateTime.of(today.plusDays(2), LocalTime.of(20, 0)),
                        "PLANNED",
                        false,
                        null
                ),
                new CoachSessionAdminView(
                        firstSessionId,
                        coachId,
                        firstGroupId,
                        UUID.randomUUID(),
                        "REGULAR",
                        today.plusDays(1),
                        LocalDateTime.of(today.plusDays(1), LocalTime.of(18, 0)),
                        LocalDateTime.of(today.plusDays(1), LocalTime.of(19, 0)),
                        "CONFIRMED",
                        true,
                        null
                )
        ));
        when(clientPort.getGroupMembers(firstGroupId)).thenReturn(List.of(
                new GroupMemberDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Player 1", LocalDate.of(2015, 1, 1), "ACTIVE", "ACTIVE", today.minusDays(10), null),
                new GroupMemberDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Player 2", LocalDate.of(2014, 1, 1), "ACTIVE", "CANCELLED", today.minusDays(20), null),
                new GroupMemberDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Player 3", LocalDate.of(2013, 1, 1), "ACTIVE", null, today.minusDays(30), null)
        ));
        when(clientPort.getGroupMembers(secondGroupId)).thenReturn(List.of());
        when(coachPort.getOverdueReportSessions(Set.of(coachId), Set.of(firstGroupId, secondGroupId), today)).thenReturn(List.of(
                new CoachSessionAdminView(
                        UUID.randomUUID(),
                        coachId,
                        secondGroupId,
                        UUID.randomUUID(),
                        "REGULAR",
                        today.minusDays(1),
                        LocalDateTime.of(today.minusDays(1), LocalTime.of(18, 0)),
                        LocalDateTime.of(today.minusDays(1), LocalTime.of(19, 0)),
                        "PLANNED",
                        false,
                        null
                )
        ));
        when(coachPort.getReportedSessions(coachId)).thenReturn(List.of());
        when(coachPort.countOverdueReports(coachId, today)).thenReturn(1);
        when(coachPort.getStatusHistory(coachId)).thenReturn(List.of(
                new CoachStatusHistoryDto(
                        "ACTIVE",
                        LocalDateTime.of(today, LocalTime.NOON),
                        adminId,
                        "ACCOUNT_STATUS_CHANGED",
                        null,
                        "ACTIVE",
                        null,
                        null,
                        null
                )
        ));

        AdminCoachProfileOutput output = service.getCoachProfile(adminId, coachId);

        assertEquals(2, output.groups().size());
        assertEquals(1, output.groupAssignmentHistory().size());
        assertEquals(3, output.weeklySchedule().size());
        assertEquals(LocalDate.of(1990, 4, 12), output.birthDate());
        assertEquals("UEFA B licensed coach", output.description());
        AdminCoachProfileOutput.GroupAssignmentHistoryItem completedAssignment = output.groupAssignmentHistory().getFirst();
        assertEquals(completedAssignmentId, completedAssignment.groupCoachId());
        assertEquals("Falcons", completedAssignment.groupName());
        assertEquals("ASSISTANT", completedAssignment.role());
        assertEquals("Schedule changed", completedAssignment.removalReason());

        AdminCoachProfileOutput.GroupItem firstGroup = output.groups().stream()
                .filter(group -> group.groupId().equals(firstGroupId))
                .findFirst()
                .orElseThrow();
        assertEquals("Falcons", firstGroup.groupName());
        assertEquals(firstGroupCoachId, firstGroup.groupCoachId());
        assertEquals("MAIN", firstGroup.role());
        assertEquals(3, firstGroup.studentsCount());
        assertEquals(2, firstGroup.activeStudentsCount());
        assertEquals(2, firstGroup.weeklySlotsCount());
        assertNotNull(firstGroup.nextSession());
        assertEquals(firstSessionId, firstGroup.nextSession().sessionId());
        assertEquals(today.plusDays(1), firstGroup.nextSession().sessionDate());
        assertEquals(LocalTime.of(18, 0), firstGroup.nextSession().startTime());
        assertEquals("CONFIRMED", firstGroup.nextSession().status());
        assertEquals(List.of(), firstGroup.riskFlags());

        AdminCoachProfileOutput.WeeklyScheduleItem mondayFalconsSlot = output.weeklySchedule().stream()
                .filter(slot -> slot.groupId().equals(firstGroupId))
                .filter(slot -> slot.dayOfWeek() == DayOfWeek.MONDAY)
                .filter(slot -> slot.startTime().equals(LocalTime.of(18, 0)))
                .findFirst()
                .orElseThrow();
        assertEquals("ACTIVE", mondayFalconsSlot.scheduleStatus());
        assertEquals("Активно", mondayFalconsSlot.scheduleStatusLabel());
        assertEquals("Aibek Coach", mondayFalconsSlot.coachName());
        assertEquals(1, mondayFalconsSlot.conflicts().size());
        assertEquals(secondGroupId, mondayFalconsSlot.conflicts().getFirst().conflictingGroupId());
        assertEquals("Wolves", mondayFalconsSlot.conflicts().getFirst().conflictingGroupName());
        assertEquals("Aibek Coach", mondayFalconsSlot.conflicts().getFirst().coachName());

        AdminCoachProfileOutput.GroupItem secondGroup = output.groups().stream()
                .filter(group -> group.groupId().equals(secondGroupId))
                .findFirst()
                .orElseThrow();
        assertEquals("Wolves", secondGroup.groupName());
        assertEquals(secondGroupCoachId, secondGroup.groupCoachId());
        assertNull(secondGroup.role());
        assertEquals(0, secondGroup.studentsCount());
        assertEquals(0, secondGroup.activeStudentsCount());
        assertEquals(1, secondGroup.weeklySlotsCount());
        assertNull(secondGroup.nextSession());
        assertEquals(
                List.of("NO_STUDENTS", "NO_UPCOMING_SESSIONS", "OVERDUE_REPORTS"),
                secondGroup.riskFlags().stream().map(AdminCoachProfileOutput.RiskFlagItem::code).toList()
        );
    }

    @Test
    void shouldReturnPaginatedOverviewWithSearchAndStatusFilter() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID activeCoachId = UUID.randomUUID();
        UUID inactiveCoachId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        MediaAsset activeCoachAvatar = MediaAsset.builder()
                .id(UUID.randomUUID())
                .ownerType(MediaOwnerType.COACH)
                .ownerId(activeCoachId)
                .kind(MediaKind.AVATAR)
                .fileName("arsen.jpg")
                .mimeType("image/jpeg")
                .sizeBytes(1024L)
                .originalStorageKey("coach/original.jpg")
                .build();
        MediaAssetResponse activeCoachAvatarResponse = new MediaAssetResponse(
                activeCoachAvatar.getId(), MediaOwnerType.COACH, activeCoachId, MediaKind.AVATAR,
                "arsen.jpg", "image/jpeg", 1024L, 256, 256,
                "/media/coach/original", "/media/coach/thumb", "/media/coach/medium", LocalDateTime.now()
        );

        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(mediaAvatarPort.findActiveAvatars(MediaOwnerType.COACH, Set.of(activeCoachId, inactiveCoachId)))
                .thenReturn(java.util.Map.of(activeCoachId, activeCoachAvatar));
        when(mediaAccessPort.toResponse(activeCoachAvatar)).thenReturn(activeCoachAvatarResponse);
        when(coachPort.getCoaches(Set.of(branchId), Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(
                CoachDto.builder()
                        .id(activeCoachId)
                        .firstName("Arsen")
                        .lastName("Gizatov")
                        .email("arsen@example.com")
                        .phone("+77770000000")
                        .specialization("U12")
                        .active(true)
                        .createdAt(LocalDateTime.of(2026, 7, 1, 10, 0))
                        .build(),
                CoachDto.builder()
                        .id(inactiveCoachId)
                        .firstName("Bek")
                        .lastName("Askarov")
                        .email("bek@example.com")
                        .phone("+77770000001")
                        .specialization("U10")
                        .active(false)
                        .createdAt(LocalDateTime.of(2026, 6, 1, 10, 0))
                        .build()
        )));
        when(groupPort.getGroupsByBranch(branchId)).thenReturn(List.of(
                GroupDto.builder().groupId(groupId).name("Falcons").branchId(branchId).status(GroupStatus.ACTIVE).build()
        ));
        when(groupCoachPort.getActiveAssignmentsByCoachIdsAndGroupIds(Set.of(activeCoachId, inactiveCoachId), Set.of(groupId)))
                .thenReturn(List.of(
                        GroupCoachDto.builder()
                                .id(UUID.randomUUID())
                                .groupId(groupId)
                                .coachId(activeCoachId)
                                .role(CoachRole.MAIN)
                                .active(true)
                                .build()
                ));
        when(coachPort.getSessions(any(), any(), any(), any())).thenReturn(List.of(
                new CoachSessionAdminView(
                        UUID.randomUUID(),
                        activeCoachId,
                        groupId,
                        UUID.randomUUID(),
                        "REGULAR",
                        today,
                        LocalDateTime.of(today, LocalTime.of(18, 0)),
                        LocalDateTime.of(today, LocalTime.of(19, 0)),
                        "PLANNED",
                        false,
                        null
                ),
                new CoachSessionAdminView(
                        UUID.randomUUID(),
                        activeCoachId,
                        groupId,
                        UUID.randomUUID(),
                        "REGULAR",
                        today.plusDays(1),
                        LocalDateTime.of(today.plusDays(1), LocalTime.of(18, 0)),
                        LocalDateTime.of(today.plusDays(1), LocalTime.of(19, 0)),
                        "PLANNED",
                        false,
                        null
                )
        ));
        when(coachPort.getOverdueReportSessions(any(), any(), any())).thenReturn(List.of());
        when(coachPort.getReportedSessions(any(), any())).thenReturn(List.of(
                new CoachSessionAdminView(
                        UUID.randomUUID(),
                        activeCoachId,
                        groupId,
                        UUID.randomUUID(),
                        "REGULAR",
                        today.minusDays(1),
                        LocalDateTime.of(today.minusDays(1), LocalTime.of(18, 0)),
                        LocalDateTime.of(today.minusDays(1), LocalTime.of(19, 0)),
                        "DONE",
                        true,
                        LocalDateTime.of(2026, 7, 9, 10, 0)
                )
        ));

        AdminCoachOverviewOutput output = service.getCoachesOverview(
                adminId,
                branchId,
                0,
                20,
                "arsen",
                "ACTIVE",
                List.of("lastName,asc", "firstName,asc")
        );

        assertEquals(2, output.summary().total());
        assertEquals(1, output.summary().active());
        assertEquals(1, output.summary().inactive());
        assertEquals(1, output.summary().withoutGroups());
        assertEquals(1, output.summary().withSessionsToday());
        assertEquals(1, output.coaches().getTotalElements());
        assertEquals(1, output.coaches().getContent().size());
        assertEquals(activeCoachId, output.coaches().getContent().getFirst().coachId());
        assertEquals("U12", output.coaches().getContent().getFirst().specialization());
        assertEquals("/media/coach/thumb", output.coaches().getContent().getFirst().avatar().thumbUrl());
        assertEquals("LOW", output.coaches().getContent().getFirst().load().status());
        assertTrue(output.coaches().getContent().getFirst().active());
    }

    @Test
    void shouldApplyStableSortingForOverview() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID coachA = UUID.randomUUID();
        UUID coachB = UUID.randomUUID();
        UUID coachC = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        LocalDate today = LocalDate.now();

        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(coachPort.getCoaches(Set.of(branchId), Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(
                CoachDto.builder().id(coachB).firstName("B").lastName("Beta").email("b@example.com").phone("2").specialization("U11").active(true).createdAt(LocalDateTime.now()).build(),
                CoachDto.builder().id(coachC).firstName("C").lastName("Gamma").email("c@example.com").phone("3").specialization("U12").active(false).createdAt(LocalDateTime.now()).build(),
                CoachDto.builder().id(coachA).firstName("A").lastName("Alpha").email("a@example.com").phone("1").specialization("U10").active(true).createdAt(LocalDateTime.now()).build()
        )));
        when(groupPort.getGroupsByBranch(branchId)).thenReturn(List.of(
                GroupDto.builder().groupId(groupId).name("Falcons").branchId(branchId).status(GroupStatus.ACTIVE).build()
        ));
        when(groupCoachPort.getActiveAssignmentsByCoachIdsAndGroupIds(Set.of(coachA, coachB, coachC), Set.of(groupId)))
                .thenReturn(List.of(
                        GroupCoachDto.builder().id(UUID.randomUUID()).groupId(groupId).coachId(coachA).role(CoachRole.MAIN).active(true).build(),
                        GroupCoachDto.builder().id(UUID.randomUUID()).groupId(groupId).coachId(coachB).role(CoachRole.MAIN).active(true).build()
                ));
        when(coachPort.getSessions(any(), any(), any(), any())).thenReturn(List.of(
                new CoachSessionAdminView(UUID.randomUUID(), coachA, groupId, UUID.randomUUID(), "REGULAR", today, LocalDateTime.of(today, LocalTime.of(18, 0)), LocalDateTime.of(today, LocalTime.of(19, 0)), "PLANNED", false, null),
                new CoachSessionAdminView(UUID.randomUUID(), coachA, groupId, UUID.randomUUID(), "REGULAR", today.plusDays(1), LocalDateTime.of(today.plusDays(1), LocalTime.of(18, 0)), LocalDateTime.of(today.plusDays(1), LocalTime.of(19, 0)), "PLANNED", false, null),
                new CoachSessionAdminView(UUID.randomUUID(), coachA, groupId, UUID.randomUUID(), "REGULAR", today.plusDays(2), LocalDateTime.of(today.plusDays(2), LocalTime.of(18, 0)), LocalDateTime.of(today.plusDays(2), LocalTime.of(19, 0)), "PLANNED", false, null),
                new CoachSessionAdminView(UUID.randomUUID(), coachA, groupId, UUID.randomUUID(), "REGULAR", today.plusDays(3), LocalDateTime.of(today.plusDays(3), LocalTime.of(18, 0)), LocalDateTime.of(today.plusDays(3), LocalTime.of(19, 0)), "PLANNED", false, null),
                new CoachSessionAdminView(UUID.randomUUID(), coachA, groupId, UUID.randomUUID(), "REGULAR", today.plusDays(4), LocalDateTime.of(today.plusDays(4), LocalTime.of(18, 0)), LocalDateTime.of(today.plusDays(4), LocalTime.of(19, 0)), "PLANNED", false, null),
                new CoachSessionAdminView(UUID.randomUUID(), coachA, groupId, UUID.randomUUID(), "REGULAR", today.plusDays(5), LocalDateTime.of(today.plusDays(5), LocalTime.of(18, 0)), LocalDateTime.of(today.plusDays(5), LocalTime.of(19, 0)), "PLANNED", false, null),
                new CoachSessionAdminView(UUID.randomUUID(), coachA, groupId, UUID.randomUUID(), "REGULAR", today.plusDays(6), LocalDateTime.of(today.plusDays(6), LocalTime.of(18, 0)), LocalDateTime.of(today.plusDays(6), LocalTime.of(19, 0)), "PLANNED", false, null),
                new CoachSessionAdminView(UUID.randomUUID(), coachA, groupId, UUID.randomUUID(), "REGULAR", today.plusDays(7), LocalDateTime.of(today.plusDays(7), LocalTime.of(18, 0)), LocalDateTime.of(today.plusDays(7), LocalTime.of(19, 0)), "PLANNED", false, null),
                new CoachSessionAdminView(UUID.randomUUID(), coachA, groupId, UUID.randomUUID(), "REGULAR", today.plusDays(8), LocalDateTime.of(today.plusDays(8), LocalTime.of(18, 0)), LocalDateTime.of(today.plusDays(8), LocalTime.of(19, 0)), "PLANNED", false, null),
                new CoachSessionAdminView(UUID.randomUUID(), coachA, groupId, UUID.randomUUID(), "REGULAR", today.plusDays(9), LocalDateTime.of(today.plusDays(9), LocalTime.of(18, 0)), LocalDateTime.of(today.plusDays(9), LocalTime.of(19, 0)), "PLANNED", false, null),
                new CoachSessionAdminView(UUID.randomUUID(), coachB, groupId, UUID.randomUUID(), "REGULAR", today, LocalDateTime.of(today, LocalTime.of(10, 0)), LocalDateTime.of(today, LocalTime.of(11, 0)), "PLANNED", false, null)
        ));
        when(coachPort.getOverdueReportSessions(any(), any(), any())).thenReturn(List.of());
        when(coachPort.getReportedSessions(any(), any())).thenReturn(List.of());

        AdminCoachOverviewOutput activeSorted = service.getCoachesOverview(
                adminId, branchId, 0, 20, null, "ALL", List.of("active,desc")
        );
        assertEquals(List.of(coachA, coachB, coachC), activeSorted.coaches().getContent().stream().map(AdminCoachOverviewOutput.CoachItem::coachId).toList());

        AdminCoachOverviewOutput loadStatusSorted = service.getCoachesOverview(
                adminId, branchId, 0, 20, null, "ALL", List.of("loadStatus,desc")
        );
        assertEquals(List.of(coachA, coachB, coachC), loadStatusSorted.coaches().getContent().stream().map(AdminCoachOverviewOutput.CoachItem::coachId).toList());

        AdminCoachOverviewOutput splitParamSorted = service.getCoachesOverview(
                adminId, branchId, 0, 20, null, "ALL", List.of("todaySessionsCount", "asc")
        );
        assertEquals(List.of(coachC, coachA, coachB), splitParamSorted.coaches().getContent().stream().map(AdminCoachOverviewOutput.CoachItem::coachId).toList());
    }
}
