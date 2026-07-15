package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.group.AdminAddGroupMemberInput;
import kz.edu.soccerhub.admin.application.dto.group.AdminGroupMemberCandidatesOutput;
import kz.edu.soccerhub.admin.application.dto.group.AdminGroupMembershipOutput;
import kz.edu.soccerhub.admin.application.dto.group.AdminGroupMembershipTransferOutput;
import kz.edu.soccerhub.admin.application.dto.group.AdminRemoveGroupMembershipInput;
import kz.edu.soccerhub.admin.application.dto.group.AdminTransferGroupMembershipInput;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.dto.student.StudentProfileDto;
import kz.edu.soccerhub.common.exception.ConflictException;
import kz.edu.soccerhub.common.port.ClientPort;
import kz.edu.soccerhub.common.port.GroupActivityPort;
import kz.edu.soccerhub.common.port.GroupMembershipPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.MediaAccessPort;
import kz.edu.soccerhub.common.port.MediaAvatarPort;
import kz.edu.soccerhub.organization.domain.model.enums.GroupAudienceType;
import kz.edu.soccerhub.organization.domain.model.GroupMembership;
import kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus;
import kz.edu.soccerhub.organization.domain.model.enums.GroupLevel;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminGroupMembershipServiceTest {

    @Mock
    private GroupMembershipPort groupMembershipPort;
    @Mock
    private GroupPort groupPort;
    @Mock
    private ClientPort clientPort;
    @Mock
    private AdminService adminService;
    @Mock
    private AdminBranchService adminBranchService;
    @Mock
    private GroupActivityPort groupActivityPort;
    @Mock
    private MediaAvatarPort mediaAvatarPort;
    @Mock
    private MediaAccessPort mediaAccessPort;

    private AdminGroupMembershipService service;

    @BeforeEach
    void setUp() {
        service = new AdminGroupMembershipService(
                groupMembershipPort,
                groupPort,
                clientPort,
                adminService,
                adminBranchService,
                groupActivityPort,
                mediaAvatarPort,
                mediaAccessPort
        );
    }

    @Test
    void shouldAddMember() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        LocalDate joinedAt = LocalDate.now();

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(groupPort.getGroupByIdForUpdate(groupId)).thenReturn(groupDto(groupId, branchId, "Adal"));
        when(clientPort.getStudentProfile(playerId)).thenReturn(player(playerId, branchId));
        when(groupMembershipPort.findByGroupIdAndPlayerIdAsOfDate(groupId, playerId, joinedAt)).thenReturn(Optional.empty());
        when(groupMembershipPort.countActiveByGroupIdAsOfDate(groupId, joinedAt)).thenReturn(10L);
        when(groupMembershipPort.save(any(GroupMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminGroupMembershipOutput output = service.addMember(
                adminId,
                groupId,
                new AdminAddGroupMemberInput(playerId, joinedAt, "NEW_ENROLLMENT", "ok")
        );

        assertEquals(groupId, output.group().id());
        assertEquals(playerId, output.player().id());
        assertEquals("ACTIVE", output.status());

        ArgumentCaptor<GroupMembership> captor = ArgumentCaptor.forClass(GroupMembership.class);
        verify(groupMembershipPort).save(captor.capture());
        assertEquals("NEW_ENROLLMENT", captor.getValue().getJoinReason());
        verify(groupActivityPort).recordGroupActivity(eq(groupId), eq(adminId), eq("STUDENT_ADDED"), any());
    }

    @Test
    void shouldRejectAddWhenCapacityExceeded() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        LocalDate joinedAt = LocalDate.now();

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(groupPort.getGroupByIdForUpdate(groupId)).thenReturn(GroupDto.builder()
                .groupId(groupId)
                .branchId(branchId)
                .name("Adal")
                .status(GroupStatus.ACTIVE)
                .audienceType(GroupAudienceType.CHILDREN)
                .ageFrom(10)
                .ageTo(15)
                .capacity(2)
                .level(GroupLevel.PRO)
                .build());
        when(clientPort.getStudentProfile(playerId)).thenReturn(player(playerId, branchId));
        when(groupMembershipPort.findByGroupIdAndPlayerIdAsOfDate(groupId, playerId, joinedAt)).thenReturn(Optional.empty());
        when(groupMembershipPort.countActiveByGroupIdAsOfDate(groupId, joinedAt)).thenReturn(2L);

        assertThrows(ConflictException.class, () -> service.addMember(
                adminId,
                groupId,
                new AdminAddGroupMemberInput(playerId, joinedAt, null, null)
        ));
    }

    @Test
    void shouldRejectAddWhenMembershipOverlapsAndProvideNextAvailableDate() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        LocalDate joinedAt = LocalDate.of(2026, 7, 15);

        GroupMembership removedMembership = GroupMembership.builder()
                .id(UUID.randomUUID())
                .groupId(groupId)
                .playerId(playerId)
                .status(GroupMembershipStatus.REMOVED)
                .joinedAt(LocalDate.of(2026, 7, 1))
                .leftAt(joinedAt)
                .build();

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(groupPort.getGroupByIdForUpdate(groupId)).thenReturn(groupDto(groupId, branchId, "Adal"));
        when(clientPort.getStudentProfile(playerId)).thenReturn(player(playerId, branchId));
        when(groupMembershipPort.findByGroupIdAndPlayerIdAsOfDate(groupId, playerId, joinedAt)).thenReturn(Optional.of(removedMembership));

        ConflictException exception = assertThrows(ConflictException.class, () -> service.addMember(
                adminId,
                groupId,
                new AdminAddGroupMemberInput(playerId, joinedAt, "NEW_ENROLLMENT", null)
        ));

        assertEquals("MEMBERSHIP_DATE_OVERLAP", exception.getErrorCode());
        assertEquals(joinedAt.plusDays(1), exception.getMetadata().get("earliestAvailableJoinDate"));
    }

    @Test
    void shouldTransferMember() {
        UUID adminId = UUID.randomUUID();
        UUID sourceGroupId = UUID.randomUUID();
        UUID targetGroupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        LocalDate joinedAt = LocalDate.of(2026, 7, 1);
        LocalDate transferDate = LocalDate.of(2026, 8, 1);

        GroupMembership current = GroupMembership.builder()
                .id(membershipId)
                .groupId(sourceGroupId)
                .playerId(playerId)
                .status(GroupMembershipStatus.ACTIVE)
                .joinedAt(joinedAt)
                .build();

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(groupMembershipPort.findByIdForUpdate(membershipId)).thenReturn(Optional.of(current));
        when(groupPort.getGroupById(sourceGroupId)).thenReturn(groupDto(sourceGroupId, branchId, "Adal"));
        when(groupPort.getGroupByIdForUpdate(targetGroupId)).thenReturn(groupDto(targetGroupId, branchId, "Tangy Football"));
        when(clientPort.getStudentProfile(playerId)).thenReturn(player(playerId, branchId));
        when(groupMembershipPort.existsActiveByGroupIdAndPlayerIdAsOfDate(targetGroupId, playerId, transferDate)).thenReturn(false);
        when(groupMembershipPort.countActiveByGroupIdAsOfDate(targetGroupId, transferDate)).thenReturn(10L);
        when(groupMembershipPort.save(any(GroupMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminGroupMembershipTransferOutput output = service.transferMember(
                adminId,
                membershipId,
                new AdminTransferGroupMembershipInput(targetGroupId, transferDate, "SCHEDULE_CHANGE", "reason")
        );

        assertEquals("TRANSFERRED", output.previousMembership().status());
        assertEquals(LocalDate.of(2026, 7, 31), output.previousMembership().leftAt());
        assertEquals(targetGroupId, output.newMembership().group().id());
        verify(groupActivityPort).recordGroupActivity(eq(sourceGroupId), eq(adminId), eq("STUDENT_TRANSFERRED"), any(), any());
        verify(groupActivityPort).recordGroupActivity(eq(targetGroupId), eq(adminId), eq("STUDENT_TRANSFERRED"), any(), any());
    }

    @Test
    void shouldRemoveMember() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        GroupMembership membership = GroupMembership.builder()
                .id(membershipId)
                .groupId(groupId)
                .playerId(playerId)
                .status(GroupMembershipStatus.ACTIVE)
                .joinedAt(LocalDate.of(2026, 7, 1))
                .build();

        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(groupMembershipPort.findByIdForUpdate(membershipId)).thenReturn(Optional.of(membership));
        when(groupPort.getGroupById(groupId)).thenReturn(groupDto(groupId, branchId, "Adal"));
        when(clientPort.getStudentProfile(playerId)).thenReturn(player(playerId, branchId));

        AdminGroupMembershipOutput output = service.removeMember(
                adminId,
                membershipId,
                new AdminRemoveGroupMembershipInput(LocalDate.of(2026, 8, 1), "PARENT_REQUEST", "comment")
        );

        assertEquals("REMOVED", output.status());
        assertEquals(LocalDate.of(2026, 8, 1), output.leftAt());
        verify(groupActivityPort).recordGroupActivity(eq(groupId), eq(adminId), eq("STUDENT_REMOVED"), any());
    }

    @Test
    void shouldReturnMemberCandidatesExcludingCurrentGroupMembers() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID otherGroupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID firstPlayerId = UUID.randomUUID();
        UUID secondPlayerId = UUID.randomUUID();
        UUID blockedPlayerId = UUID.randomUUID();
        UUID otherMembershipId = UUID.randomUUID();

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
        when(clientPort.getStudentProfilesByBranch(branchId)).thenReturn(List.of(
                new StudentProfileDto(branchId, firstPlayerId, "Алихан Сериков", null, LocalDate.of(2015, 4, 12), UUID.randomUUID(), "Parent One", "+7701", "a@test.com", "ACTIVE"),
                new StudentProfileDto(branchId, secondPlayerId, "Диас Ахметов", null, LocalDate.of(2018, 2, 10), UUID.randomUUID(), "Parent Two", "+7702", "b@test.com", "ACTIVE"),
                new StudentProfileDto(branchId, blockedPlayerId, "Елена Садыкова", null, LocalDate.of(2015, 8, 20), UUID.randomUUID(), "Parent Three", "+7703", "c@test.com", "ACTIVE")
        ));
        when(groupMembershipPort.findActiveByPlayerIdInAsOfDate(any(), any())).thenReturn(List.of(
                GroupMembership.builder()
                        .id(UUID.randomUUID())
                        .groupId(groupId)
                        .playerId(firstPlayerId)
                        .status(GroupMembershipStatus.ACTIVE)
                        .joinedAt(LocalDate.now().minusDays(10))
                        .build(),
                GroupMembership.builder()
                        .id(otherMembershipId)
                        .groupId(otherGroupId)
                        .playerId(secondPlayerId)
                        .status(GroupMembershipStatus.ACTIVE)
                        .joinedAt(LocalDate.now().minusDays(5))
                        .leftAt(LocalDate.now().plusDays(30))
                        .build()
        ));
        when(groupMembershipPort.findByGroupIdAndPlayerIdInEndingOnOrAfterDate(eq(groupId), any(), any())).thenReturn(List.of(
                GroupMembership.builder()
                        .id(UUID.randomUUID())
                        .groupId(groupId)
                        .playerId(secondPlayerId)
                        .status(GroupMembershipStatus.REMOVED)
                        .joinedAt(LocalDate.now().minusDays(1))
                        .leftAt(LocalDate.now())
                        .build(),
                GroupMembership.builder()
                        .id(UUID.randomUUID())
                        .groupId(groupId)
                        .playerId(blockedPlayerId)
                        .status(GroupMembershipStatus.REMOVED)
                        .joinedAt(LocalDate.now().minusDays(20))
                        .build()
        ));
        when(groupPort.getGroupsByIds(Set.of(groupId, otherGroupId))).thenReturn(List.of(
                groupDto(groupId, branchId, "Adal"),
                groupDto(otherGroupId, branchId, "Tangy Football")
        ));

        AdminGroupMemberCandidatesOutput output = service.getMemberCandidates(adminId, groupId, null, 0, 20);

        assertEquals(2, output.total());
        AdminGroupMemberCandidatesOutput.Item available = output.items().stream()
                .filter(item -> secondPlayerId.equals(item.playerId()))
                .findFirst()
                .orElseThrow();
        assertEquals(otherMembershipId, available.currentMemberships().getFirst().membershipId());
        assertEquals("Tangy Football", available.currentMemberships().getFirst().groupName());
        assertEquals(LocalDate.now().plusDays(30), available.currentMemberships().getFirst().leftAt());
        assertEquals("PLAYER_AGE_OUTSIDE_GROUP_RANGE", available.warnings().getFirst().code());
        assertEquals(LocalDate.now().plusDays(1), available.earliestAvailableJoinDate());
        assertEquals("MEMBERSHIP_AVAILABLE_FROM", available.warnings().get(1).code());

        AdminGroupMemberCandidatesOutput.Item blocked = output.items().stream()
                .filter(item -> blockedPlayerId.equals(item.playerId()))
                .findFirst()
                .orElseThrow();
        assertFalse(blocked.eligible());
        assertEquals("PLAYER_ALREADY_IN_GROUP", blocked.warnings().getFirst().code());
    }

    private StudentProfileDto player(UUID playerId, UUID branchId) {
        return new StudentProfileDto(
                branchId,
                playerId,
                "Алихан Сериков",
                null,
                LocalDate.of(2015, 4, 12),
                UUID.randomUUID(),
                "Parent One",
                "+7701",
                "a@test.com",
                "ACTIVE"
        );
    }

    private GroupDto groupDto(UUID groupId, UUID branchId, String name) {
        return GroupDto.builder()
                .groupId(groupId)
                .branchId(branchId)
                .name(name)
                .status(GroupStatus.ACTIVE)
                .audienceType(GroupAudienceType.CHILDREN)
                .ageFrom(10)
                .ageTo(15)
                .capacity(20)
                .level(GroupLevel.PRO)
                .build();
    }
}
