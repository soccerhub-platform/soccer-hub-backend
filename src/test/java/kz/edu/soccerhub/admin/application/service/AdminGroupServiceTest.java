package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.group.AdminGroupDetailsOutput;
import kz.edu.soccerhub.admin.application.dto.group.AdminGroupCoachOutput;
import kz.edu.soccerhub.admin.application.dto.group.AdminGroupMemberOutput;
import kz.edu.soccerhub.admin.application.dto.group.AdminGroupUpdateInput;
import kz.edu.soccerhub.admin.application.dto.group.AdminRemoveGroupCoachInput;
import kz.edu.soccerhub.admin.application.dto.group.GroupHealth;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.dto.client.GroupMemberDto;
import kz.edu.soccerhub.common.dto.coach.PlayerAttendanceRateDto;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.dto.coach.CoachSessionAdminView;
import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;
import kz.edu.soccerhub.common.dto.group.GroupCoachDto;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.dto.group.GroupScheduleDto;
import kz.edu.soccerhub.common.exception.ConflictException;
import kz.edu.soccerhub.common.dto.group.UpdateGroupCommand;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.port.BranchPort;
import kz.edu.soccerhub.common.port.ClientPort;
import kz.edu.soccerhub.common.port.CoachAvailabilityPort;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.GroupActivityPort;
import kz.edu.soccerhub.common.port.GroupCoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.GroupSchedulePort;
import kz.edu.soccerhub.common.port.MediaAccessPort;
import kz.edu.soccerhub.common.port.MediaAvatarPort;
import kz.edu.soccerhub.organization.application.service.GroupScheduleValidationService;
import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;
import kz.edu.soccerhub.organization.domain.model.enums.GroupAudienceType;
import kz.edu.soccerhub.organization.domain.model.enums.GroupLevel;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import kz.edu.soccerhub.coach.domain.model.enums.AccountStatus;
import kz.edu.soccerhub.coach.domain.model.enums.WorkStatus;
import kz.edu.soccerhub.media.domain.enums.MediaKind;
import kz.edu.soccerhub.media.domain.enums.MediaOwnerType;
import kz.edu.soccerhub.media.domain.model.MediaAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AdminGroupServiceTest {

    @Mock
    private GroupPort groupPort;
    @Mock
    private CoachPort coachPort;
    @Mock
    private GroupCoachPort groupCoachPort;
    @Mock
    private GroupSchedulePort groupSchedulePort;
    @Mock
    private CoachAvailabilityPort coachAvailabilityPort;
    @Mock
    private ClientPort clientPort;
    @Mock
    private GroupScheduleValidationService groupScheduleValidationService;
    @Mock
    private AdminService adminService;
    @Mock
    private AdminBranchService adminBranchService;
    @Mock
    private BranchPort branchPort;
    @Mock
    private GroupActivityPort groupActivityPort;
    @Mock
    private MediaAvatarPort mediaAvatarPort;
    @Mock
    private MediaAccessPort mediaAccessPort;

    private AdminGroupService service;

    @BeforeEach
    void setUp() {
        service = new AdminGroupService(
                groupPort,
                coachPort,
                groupCoachPort,
                groupSchedulePort,
                coachAvailabilityPort,
                clientPort,
                groupScheduleValidationService,
                adminService,
                adminBranchService,
                branchPort,
                groupActivityPort,
                mediaAvatarPort,
                mediaAccessPort
        );
    }

    @Test
    void shouldAssignCoachWithPeriodThroughCanonicalGroupFlow() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        LocalDate assignedFrom = LocalDate.now().plusDays(2);
        LocalDate assignedTo = assignedFrom.plusMonths(3);

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(groupPort.getGroupById(groupId)).thenReturn(GroupDto.builder().groupId(groupId).branchId(branchId).build());
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(coachPort.verifyCoach(coachId)).thenReturn(true);
        when(coachPort.getBranchIds(coachId)).thenReturn(Set.of(branchId));
        when(groupCoachPort.assignCoach(groupId, coachId, CoachRole.MAIN, assignedFrom, assignedTo))
                .thenReturn(assignmentId);

        UUID result = service.assignCoachToGroup(
                adminId,
                groupId,
                coachId,
                CoachRole.MAIN,
                assignedFrom,
                assignedTo
        );

        assertEquals(assignmentId, result);
        verify(groupCoachPort).assignCoach(groupId, coachId, CoachRole.MAIN, assignedFrom, assignedTo);
    }

    @Test
    void shouldAllowGroupAssignmentWhenCoachScheduleConflicts() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusMonths(2);

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(groupPort.getGroupById(groupId)).thenReturn(GroupDto.builder().groupId(groupId).branchId(branchId).build());
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(coachPort.verifyCoach(coachId)).thenReturn(true);
        when(coachPort.getBranchIds(coachId)).thenReturn(Set.of(branchId));
        when(groupCoachPort.assignCoach(groupId, coachId, CoachRole.ASSISTANT, from, to))
                .thenReturn(UUID.randomUUID());

        service.assignCoachToGroup(
                adminId,
                groupId,
                coachId,
                CoachRole.ASSISTANT,
                from,
                to
        );

        verify(groupCoachPort).assignCoach(groupId, coachId, CoachRole.ASSISTANT, from, to);
        verifyNoInteractions(groupSchedulePort);
    }

    @Test
    void shouldReplaceFutureWorkAndPromoteReplacementWhenRemovingMainCoach() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        UUID replacementAssignmentId = UUID.randomUUID();
        UUID replacementCoachId = UUID.randomUUID();
        LocalDate effectiveDate = LocalDate.now();
        GroupCoachDto assignment = GroupCoachDto.builder()
                .id(assignmentId).groupId(groupId).coachId(coachId).role(CoachRole.MAIN).active(true).build();
        GroupCoachDto replacement = GroupCoachDto.builder()
                .id(replacementAssignmentId).groupId(groupId).coachId(replacementCoachId)
                .role(CoachRole.ASSISTANT).active(true).build();
        GroupScheduleDto schedule = GroupScheduleDto.builder()
                .scheduleId(UUID.randomUUID()).groupId(groupId).coachId(coachId)
                .dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(18, 0)).endTime(LocalTime.of(19, 0))
                .startDate(effectiveDate.minusDays(10)).endDate(effectiveDate.plusMonths(2)).build();

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(groupCoachPort.findAssignmentById(assignmentId)).thenReturn(Optional.of(assignment));
        when(groupPort.getGroupById(groupId)).thenReturn(GroupDto.builder().groupId(groupId).branchId(branchId).build());
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(groupSchedulePort.getActiveSchedulesByGroup(groupId)).thenReturn(List.of(schedule));
        when(coachPort.getSessions(Set.of(coachId), Set.of(groupId), effectiveDate, effectiveDate.plusDays(90)))
                .thenReturn(List.of());
        when(groupCoachPort.getActiveCoaches(groupId)).thenReturn(List.of(assignment, replacement));
        when(coachPort.verifyCoach(replacementCoachId)).thenReturn(true);
        when(groupSchedulePort.getActiveSchedulesByCoach(replacementCoachId)).thenReturn(List.of());
        when(groupCoachPort.unassignCoach(assignmentId, effectiveDate, "Смена тренера", replacementCoachId)).thenReturn(true);

        service.removeCoachFromGroup(
                adminId,
                assignmentId,
                new AdminRemoveGroupCoachInput(replacementCoachId, effectiveDate, "Смена тренера")
        );

        verify(groupSchedulePort).replaceCoach(groupId, coachId, replacementCoachId, effectiveDate);
        verify(groupCoachPort).unassignCoach(assignmentId, effectiveDate, "Смена тренера", replacementCoachId);
        verify(groupCoachPort).updateRole(replacementAssignmentId, CoachRole.MAIN);
    }

    @Test
    void shouldBuildOperationalGroupCoachReadModel() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        LocalDateTime nextStart = today.plusDays(1).atTime(18, 0);
        MediaAsset avatar = mock(MediaAsset.class);
        MediaAssetResponse avatarResponse = new MediaAssetResponse(
                UUID.randomUUID(), MediaOwnerType.COACH, coachId, MediaKind.AVATAR,
                "coach.jpg", "image/jpeg", 1024L, 128, 128,
                "/original", "/thumb", "/medium", LocalDateTime.now()
        );

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(groupPort.getGroupById(groupId)).thenReturn(GroupDto.builder()
                .groupId(groupId).branchId(branchId).name("Adal").build());
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(groupCoachPort.getActiveCoaches(groupId)).thenReturn(List.of(
                GroupCoachDto.builder().id(assignmentId).groupId(groupId).coachId(coachId)
                        .role(CoachRole.MAIN).active(true).assignedFrom(today).build()
        ));
        when(coachPort.getCoaches(Set.of(coachId))).thenReturn(List.of(
                CoachDto.builder().id(coachId).firstName("Арсен").lastName("Гизатов")
                        .email("arsen@example.com").specialization("Футбол")
                        .active(true).accountStatus(AccountStatus.ACTIVE).workStatus(WorkStatus.AVAILABLE).build()
        ));
        when(mediaAvatarPort.findActiveAvatars(MediaOwnerType.COACH, Set.of(coachId)))
                .thenReturn(java.util.Map.of(coachId, avatar));
        when(mediaAccessPort.toResponse(avatar)).thenReturn(avatarResponse);
        when(groupPort.getGroupsByBranch(branchId)).thenReturn(List.of(
                GroupDto.builder().groupId(groupId).branchId(branchId).name("Adal").build()
        ));
        when(groupCoachPort.getActiveAssignmentsByCoachIdsAndGroupIds(Set.of(coachId), Set.of(groupId)))
                .thenReturn(List.of(GroupCoachDto.builder().id(assignmentId).groupId(groupId).coachId(coachId).active(true).build()));
        CoachSessionAdminView nextSession = new CoachSessionAdminView(
                sessionId, coachId, groupId, UUID.randomUUID(), "REGULAR", today.plusDays(1),
                nextStart, nextStart.plusHours(1), "PLANNED", false, LocalDateTime.now()
        );
        when(coachPort.getSessions(
                org.mockito.ArgumentMatchers.eq(Set.of(coachId)),
                org.mockito.ArgumentMatchers.eq(Set.of(groupId)),
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                org.mockito.ArgumentMatchers.any(LocalDate.class)
        )).thenReturn(List.of(nextSession));

        List<AdminGroupCoachOutput> result = List.copyOf(service.getGroupCoaches(adminId, groupId));

        assertEquals(1, result.size());
        AdminGroupCoachOutput coach = result.getFirst();
        assertEquals(avatarResponse, coach.avatar());
        assertEquals("AVAILABLE", coach.workStatus());
        assertEquals(1, coach.load().groupsCount());
        assertEquals(1, coach.load().weeklySessionsCount());
        assertEquals(sessionId, coach.nextSession().sessionId());
        assertTrue(coach.capabilities().canOpenProfile());
        assertTrue(coach.capabilities().canChangeRole());
        assertTrue(coach.capabilities().canUnassign());
    }

    @Test
    void shouldBuildGroupDetailsForOverviewHeader() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(groupPort.getGroupById(groupId)).thenReturn(GroupDto.builder()
                .groupId(groupId)
                .branchId(branchId)
                .name("Adal")
                .description("PRO group")
                .status(GroupStatus.ACTIVE)
                .audienceType(GroupAudienceType.CHILDREN)
                .ageFrom(10)
                .ageTo(15)
                .capacity(20)
                .level(GroupLevel.PRO)
                .build());
        when(branchPort.findById(branchId)).thenReturn(Optional.of(
                BranchDto.builder().id(branchId).name("Главный филиал").build()
        ));
        when(groupCoachPort.getActiveCoaches(groupId)).thenReturn(List.of(
                GroupCoachDto.builder().id(UUID.randomUUID()).groupId(groupId).coachId(coachId).role(CoachRole.MAIN).active(true).build()
        ));
        when(groupSchedulePort.getActiveSchedulesByGroup(groupId)).thenReturn(List.of(
                GroupScheduleDto.builder()
                        .scheduleId(UUID.randomUUID())
                        .groupId(groupId)
                        .coachId(coachId)
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .startTime(LocalTime.of(20, 0))
                        .endTime(LocalTime.of(21, 0))
                        .startDate(LocalDate.now().minusDays(1))
                        .endDate(LocalDate.now().plusMonths(1))
                        .build(),
                GroupScheduleDto.builder()
                        .scheduleId(UUID.randomUUID())
                        .groupId(groupId)
                        .coachId(coachId)
                        .dayOfWeek(DayOfWeek.WEDNESDAY)
                        .startTime(LocalTime.of(20, 0))
                        .endTime(LocalTime.of(21, 0))
                        .startDate(LocalDate.now().minusDays(1))
                        .endDate(LocalDate.now().plusMonths(1))
                        .build()
        ));
        when(clientPort.getGroupMembers(groupId)).thenReturn(List.of(
                new GroupMemberDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "A", LocalDate.of(2015, 1, 1), "ACTIVE", "ACTIVE", LocalDate.now().minusDays(10), null),
                new GroupMemberDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "B", LocalDate.of(2014, 1, 1), "ACTIVE", "ACTIVE", LocalDate.now().minusDays(8), null)
        ));

        AdminGroupDetailsOutput result = service.getGroupDetails(adminId, groupId);

        assertEquals(groupId, result.id());
        assertEquals("Главный филиал", result.branch().name());
        assertEquals(2, result.summary().studentsCount());
        assertEquals(1, result.summary().coachesCount());
        assertEquals(2, result.summary().sessionsPerWeek());
        assertEquals(10, result.summary().occupancyPercent());
        assertEquals(GroupStatus.ACTIVE, result.status());
        assertEquals(GroupHealth.OK, result.health());
        assertNotNull(result.nextSession());
        assertTrue(result.capabilities().canEdit());
        assertTrue(result.capabilities().canPause());
        assertFalse(result.capabilities().canResume());
        assertTrue(result.capabilities().canAddStudent());
    }

    @Test
    void shouldMergePatchInputAndPersistUpdatedGroup() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(groupPort.getGroupById(groupId)).thenReturn(GroupDto.builder()
                .groupId(groupId)
                .branchId(branchId)
                .name("Adal")
                .description("Old")
                .status(GroupStatus.ACTIVE)
                .audienceType(GroupAudienceType.CHILDREN)
                .ageFrom(10)
                .ageTo(15)
                .capacity(20)
                .level(GroupLevel.BEGINNER)
                .build());
        when(clientPort.getGroupMembers(groupId)).thenReturn(List.of(
                new GroupMemberDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "A", LocalDate.of(2015, 1, 1), "ACTIVE", "ACTIVE", LocalDate.now(), null)
        ));

        service.updateGroup(adminId, groupId, new AdminGroupUpdateInput(
                "Adal PRO",
                null,
                null,
                null,
                16,
                null,
                null,
                GroupLevel.PRO
        ));

        ArgumentCaptor<UpdateGroupCommand> captor = ArgumentCaptor.forClass(UpdateGroupCommand.class);
        verify(groupPort).updateGroup(eq(groupId), captor.capture());

        UpdateGroupCommand command = captor.getValue();
        assertEquals("Adal PRO", command.name());
        assertEquals("Old", command.description());
        assertEquals(branchId, command.branchId());
        assertEquals(10, command.ageFrom());
        assertEquals(16, command.ageTo());
        assertEquals(GroupAudienceType.CHILDREN, command.audienceType());
        assertEquals(20, command.capacity());
        assertEquals(GroupLevel.PRO, command.level());
    }

    @Test
    void shouldRejectCapacityBelowCurrentMembersCount() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(groupPort.getGroupById(groupId)).thenReturn(GroupDto.builder()
                .groupId(groupId)
                .branchId(branchId)
                .name("Adal")
                .status(GroupStatus.ACTIVE)
                .audienceType(GroupAudienceType.CHILDREN)
                .ageFrom(10)
                .ageTo(15)
                .capacity(20)
                .level(GroupLevel.PRO)
                .build());
        when(clientPort.getGroupMembers(groupId)).thenReturn(List.of(
                new GroupMemberDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "A", LocalDate.of(2015, 1, 1), "ACTIVE", "ACTIVE", LocalDate.now(), null),
                new GroupMemberDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "B", LocalDate.of(2015, 1, 1), "ACTIVE", "ACTIVE", LocalDate.now(), null)
        ));

        assertThrows(BadRequestException.class, () -> service.updateGroup(
                adminId,
                groupId,
                new AdminGroupUpdateInput(null, null, null, null, null, null, 1, null)
        ));
    }

    @Test
    void shouldExposeMembershipMetadataInGroupMembersReadModel() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(groupPort.getGroupById(groupId)).thenReturn(GroupDto.builder()
                .groupId(groupId)
                .branchId(branchId)
                .name("Adal")
                .status(GroupStatus.ACTIVE)
                .audienceType(GroupAudienceType.CHILDREN)
                .ageFrom(10)
                .ageTo(15)
                .capacity(20)
                .level(GroupLevel.PRO)
                .build());
        when(clientPort.getGroupMembers(groupId)).thenReturn(List.of(
                new GroupMemberDto(
                        membershipId,
                        clientId,
                        playerId,
                        "Алихан Сериков",
                        LocalDate.of(2015, 4, 12),
                        "ACTIVE",
                        "ACTIVE",
                        LocalDate.of(2026, 7, 1),
                        null
                )
        ));
        when(coachPort.getAttendanceRates(groupId, java.util.Set.of(playerId))).thenReturn(List.of(
                new PlayerAttendanceRateDto(playerId, 83)
        ));
        when(mediaAvatarPort.findActiveAvatars(eq(kz.edu.soccerhub.media.domain.enums.MediaOwnerType.PLAYER), eq(java.util.Set.of(playerId))))
                .thenReturn(java.util.Map.of());

        Page<AdminGroupMemberOutput> result = service.getGroupMembers(adminId, groupId, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        AdminGroupMemberOutput item = result.getContent().getFirst();
        assertEquals(membershipId, item.membershipId());
        assertEquals(clientId, item.clientId());
        assertEquals(playerId, item.playerId());
        assertEquals("ACTIVE", item.membershipStatus());
        assertEquals("ACTIVE", item.contractStatus());
        assertEquals(83, item.attendanceRate());
        assertEquals(11, item.age());
        assertEquals(playerId, item.player().id());
        assertEquals("Алихан Сериков", item.player().fullName());
        assertEquals(membershipId, item.membership().id());
        assertEquals("ACTIVE", item.membership().status());
        assertEquals(83, item.stats().attendanceRate());
        assertTrue(item.capabilities().canOpenProfile());
        assertTrue(item.capabilities().canAddToAnotherGroup());
        assertTrue(item.capabilities().canTransfer());
        assertTrue(item.capabilities().canRemove());
    }

    @Test
    void shouldDisableActionsForClosedMembershipsInGroupMembersReadModel() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(groupPort.getGroupById(groupId)).thenReturn(GroupDto.builder()
                .groupId(groupId)
                .branchId(branchId)
                .name("Adal")
                .status(GroupStatus.ACTIVE)
                .audienceType(GroupAudienceType.CHILDREN)
                .ageFrom(10)
                .ageTo(15)
                .capacity(20)
                .level(GroupLevel.PRO)
                .build());
        when(clientPort.getGroupMembers(groupId)).thenReturn(List.of(
                new GroupMemberDto(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Удаленный ученик",
                        LocalDate.of(2015, 4, 12),
                        "REMOVED",
                        "ACTIVE",
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 15)
                )
        ));
        when(coachPort.getAttendanceRates(eq(groupId), org.mockito.ArgumentMatchers.anySet())).thenReturn(List.of());
        when(mediaAvatarPort.findActiveAvatars(eq(kz.edu.soccerhub.media.domain.enums.MediaOwnerType.PLAYER), org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(java.util.Map.of());

        Page<AdminGroupMemberOutput> result = service.getGroupMembers(adminId, groupId, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        AdminGroupMemberOutput item = result.getContent().getFirst();
        assertEquals("REMOVED", item.membershipStatus());
        assertFalse(item.capabilities().canTransfer());
        assertFalse(item.capabilities().canRemove());
    }
}
