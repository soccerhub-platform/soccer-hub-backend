package kz.edu.soccerhub.admin.api;

import kz.edu.soccerhub.admin.application.dto.group.AdminGroupDetailsOutput;
import kz.edu.soccerhub.admin.application.dto.group.AdminAddGroupMemberInput;
import kz.edu.soccerhub.admin.application.dto.group.AdminGroupMemberCandidatesOutput;
import kz.edu.soccerhub.admin.application.dto.group.AdminGroupMembershipOutput;
import kz.edu.soccerhub.admin.application.dto.group.AdminGroupMembershipTransferOutput;
import kz.edu.soccerhub.admin.application.dto.group.AdminRemoveGroupMembershipInput;
import kz.edu.soccerhub.admin.application.dto.group.AdminTransferGroupMembershipInput;
import kz.edu.soccerhub.admin.application.dto.group.AdminGroupUpdateInput;
import kz.edu.soccerhub.admin.application.dto.group.GroupHealth;
import kz.edu.soccerhub.admin.application.service.AdminGroupMembershipService;
import kz.edu.soccerhub.admin.application.service.AdminGroupService;
import kz.edu.soccerhub.organization.domain.model.enums.GroupAudienceType;
import kz.edu.soccerhub.organization.domain.model.enums.GroupLevel;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminGroupControllerTest {

    private final AdminGroupService adminGroupService = Mockito.mock(AdminGroupService.class);
    private final AdminGroupMembershipService adminGroupMembershipService = Mockito.mock(AdminGroupMembershipService.class);

    private AdminGroupController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminGroupController(adminGroupService, adminGroupMembershipService);
    }

    @Test
    void shouldReturnGroupDetails() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(adminId.toString());

        AdminGroupDetailsOutput output = new AdminGroupDetailsOutput(
                groupId,
                "Adal",
                "PRO group",
                GroupStatus.ACTIVE,
                10,
                15,
                GroupAudienceType.CHILDREN,
                GroupLevel.PRO,
                20,
                new AdminGroupDetailsOutput.BranchRef(branchId, "Главный филиал"),
                new AdminGroupDetailsOutput.Summary(2, 1, 2, 10),
                GroupHealth.OK,
                List.of(),
                new AdminGroupDetailsOutput.NextSession(OffsetDateTime.parse("2026-07-12T20:00:00+05:00")),
                new AdminGroupDetailsOutput.Capabilities(true, true, false, true, true)
        );

        when(adminGroupService.getGroupDetails(eq(adminId), eq(groupId))).thenReturn(output);

        assertSame(output, controller.getGroupDetails(jwt, groupId).getBody());
        verify(adminGroupService).getGroupDetails(eq(adminId), eq(groupId));
    }

    @Test
    void shouldForwardPatchToService() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(adminId.toString());

        AdminGroupUpdateInput input = new AdminGroupUpdateInput(
                "Adal PRO",
                "Updated",
                null,
                10,
                16,
                GroupAudienceType.CHILDREN,
                24,
                GroupLevel.PRO
        );

        assertEquals(204, controller.updateGroup(jwt, groupId, input).getStatusCode().value());
        verify(adminGroupService).updateGroup(eq(adminId), eq(groupId), eq(input));
    }

    @Test
    void shouldForwardAddMemberToService() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(adminId.toString());

        AdminAddGroupMemberInput input = new AdminAddGroupMemberInput(playerId, java.time.LocalDate.of(2026, 7, 15), "NEW_ENROLLMENT", null);
        AdminGroupMembershipOutput output = new AdminGroupMembershipOutput(
                UUID.randomUUID(),
                new AdminGroupMembershipOutput.GroupRef(groupId, "Adal"),
                new AdminGroupMembershipOutput.PlayerRef(playerId, "Алихан Сериков", java.time.LocalDate.of(2015, 4, 12)),
                "ACTIVE",
                java.time.LocalDate.of(2026, 7, 15),
                null,
                "NEW_ENROLLMENT",
                null,
                null,
                null
        );

        when(adminGroupMembershipService.addMember(eq(adminId), eq(groupId), eq(input))).thenReturn(output);

        assertSame(output, controller.addGroupMember(jwt, groupId, input).getBody());
        verify(adminGroupMembershipService).addMember(eq(adminId), eq(groupId), eq(input));
    }

    @Test
    void shouldForwardMemberCandidatesToService() {
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(adminId.toString());

        AdminGroupMemberCandidatesOutput output = new AdminGroupMemberCandidatesOutput(
                groupId,
                List.of(),
                0,
                0,
                20
        );

        when(adminGroupMembershipService.getMemberCandidates(eq(adminId), eq(groupId), eq("Ali"), eq(0), eq(20))).thenReturn(output);

        assertSame(output, controller.getGroupMemberCandidates(jwt, groupId, "Ali", 0, 20).getBody());
        verify(adminGroupMembershipService).getMemberCandidates(eq(adminId), eq(groupId), eq("Ali"), eq(0), eq(20));
    }

    @Test
    void shouldForwardTransferMemberToService() {
        UUID adminId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID targetGroupId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(adminId.toString());

        AdminTransferGroupMembershipInput input = new AdminTransferGroupMembershipInput(targetGroupId, java.time.LocalDate.of(2026, 8, 1), "SCHEDULE_CHANGE", null);
        AdminGroupMembershipTransferOutput output = new AdminGroupMembershipTransferOutput(
                new AdminGroupMembershipOutput(
                        UUID.randomUUID(),
                        new AdminGroupMembershipOutput.GroupRef(UUID.randomUUID(), "Adal"),
                        new AdminGroupMembershipOutput.PlayerRef(playerId, "Алихан Сериков", java.time.LocalDate.of(2015, 4, 12)),
                        "TRANSFERRED",
                        java.time.LocalDate.of(2026, 7, 1),
                        java.time.LocalDate.of(2026, 7, 31),
                        null,
                        "SCHEDULE_CHANGE",
                        null,
                        null
                ),
                new AdminGroupMembershipOutput(
                        UUID.randomUUID(),
                        new AdminGroupMembershipOutput.GroupRef(targetGroupId, "Tangy Football"),
                        new AdminGroupMembershipOutput.PlayerRef(playerId, "Алихан Сериков", java.time.LocalDate.of(2015, 4, 12)),
                        "ACTIVE",
                        java.time.LocalDate.of(2026, 8, 1),
                        null,
                        "SCHEDULE_CHANGE",
                        null,
                        null,
                        null
                )
        );

        when(adminGroupMembershipService.transferMember(eq(adminId), eq(membershipId), eq(input))).thenReturn(output);

        assertSame(output, controller.transferGroupMember(jwt, membershipId, input).getBody());
        verify(adminGroupMembershipService).transferMember(eq(adminId), eq(membershipId), eq(input));
    }

    @Test
    void shouldForwardRemoveMemberToService() {
        UUID adminId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(adminId.toString());

        AdminRemoveGroupMembershipInput input = new AdminRemoveGroupMembershipInput(java.time.LocalDate.of(2026, 8, 1), "PARENT_REQUEST", "Переезд");
        AdminGroupMembershipOutput output = new AdminGroupMembershipOutput(
                membershipId,
                new AdminGroupMembershipOutput.GroupRef(groupId, "Adal"),
                new AdminGroupMembershipOutput.PlayerRef(playerId, "Алихан Сериков", java.time.LocalDate.of(2015, 4, 12)),
                "REMOVED",
                java.time.LocalDate.of(2026, 7, 1),
                java.time.LocalDate.of(2026, 8, 1),
                null,
                "PARENT_REQUEST",
                "Переезд",
                null
        );

        when(adminGroupMembershipService.removeMember(eq(adminId), eq(membershipId), eq(input))).thenReturn(output);

        assertSame(output, controller.removeGroupMember(jwt, membershipId, input).getBody());
        verify(adminGroupMembershipService).removeMember(eq(adminId), eq(membershipId), eq(input));
    }
}
