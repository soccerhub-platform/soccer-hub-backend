package kz.edu.soccerhub.admin.api;

import kz.edu.soccerhub.admin.application.dto.group.AdminGroupDetailsOutput;
import kz.edu.soccerhub.admin.application.dto.group.AdminGroupUpdateInput;
import kz.edu.soccerhub.admin.application.dto.group.GroupHealth;
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

    private AdminGroupController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminGroupController(adminGroupService);
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
}
