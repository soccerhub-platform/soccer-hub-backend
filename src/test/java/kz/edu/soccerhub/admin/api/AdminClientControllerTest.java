package kz.edu.soccerhub.admin.api;

import kz.edu.soccerhub.admin.application.dto.client.AdminClientCreateInput;
import kz.edu.soccerhub.admin.application.dto.client.AdminClientStatusInput;
import kz.edu.soccerhub.admin.application.dto.client.AdminClientUpdateInput;
import kz.edu.soccerhub.admin.application.service.AdminClientReadService;
import kz.edu.soccerhub.admin.application.service.AdminClientWriteService;
import kz.edu.soccerhub.admin.application.service.AdminClientActivityService;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceDetailsOutput;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceListQuery;
import kz.edu.soccerhub.common.dto.client.ClientWorkspacePageOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.math.BigDecimal;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminClientControllerTest {

    private final AdminClientReadService readService = Mockito.mock(AdminClientReadService.class);
    private final AdminClientWriteService writeService = Mockito.mock(AdminClientWriteService.class);
    private final AdminClientActivityService activityService = Mockito.mock(AdminClientActivityService.class);
    private AdminClientController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminClientController(readService, writeService, activityService);
    }

    @Test
    void shouldForwardClientWriteCommands() {
        UUID adminId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        Jwt jwt = Mockito.mock(Jwt.class);
        ClientWorkspaceDetailsOutput output = new ClientWorkspaceDetailsOutput(
                new ClientWorkspaceDetailsOutput.ClientBlock(
                        clientId, branchId, "Roman", "Roman", null, null, null,
                        "NEW", null, null, null
                ),
                new ClientWorkspaceDetailsOutput.SummaryBlock(0, 0, 0, null),
                List.of(),
                new ClientWorkspaceDetailsOutput.CapabilitiesBlock(true, true, true, false, true, false, true)
        );
        AdminClientCreateInput createInput = new AdminClientCreateInput(
                branchId, "Roman", null, null, null, null, null
        );
        AdminClientUpdateInput updateInput = new AdminClientUpdateInput(
                "Roman", null, null, null, null, null
        );
        AdminClientStatusInput statusInput = new AdminClientStatusInput("ACTIVE");
        when(jwt.getSubject()).thenReturn(adminId.toString());
        when(writeService.create(adminId, createInput)).thenReturn(output);
        when(writeService.update(adminId, clientId, updateInput)).thenReturn(output);
        when(writeService.changeStatus(adminId, clientId, statusInput)).thenReturn(output);

        assertSame(output, controller.create(jwt, createInput).getBody());
        assertSame(output, controller.update(jwt, clientId, updateInput).getBody());
        assertSame(output, controller.changeStatus(jwt, clientId, statusInput).getBody());

        verify(writeService).create(adminId, createInput);
        verify(writeService).update(adminId, clientId, updateInput);
        verify(writeService).changeStatus(adminId, clientId, statusInput);
    }

    @Test
    void shouldForwardClientListQueryAndPageable() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        Jwt jwt = Mockito.mock(Jwt.class);
        PageRequest pageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "outstandingAmount"));
        ClientWorkspaceListQuery query = new ClientWorkspaceListQuery(
                "roman", Set.of("ACTIVE"), "WITH_STUDENTS", "ACTIVE", "DEBT"
        );
        ClientWorkspacePageOutput output = new ClientWorkspacePageOutput(
                List.of(), 0, 0, 0, 10,
                new ClientWorkspacePageOutput.Summary(0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, "KZT", false)
        );
        when(jwt.getSubject()).thenReturn(adminId.toString());
        when(readService.getClients(adminId, branchId, query, pageable)).thenReturn(output);

        assertSame(output, controller.getClients(
                jwt, branchId, "roman", Set.of("ACTIVE"), "WITH_STUDENTS", "ACTIVE", "DEBT", pageable
        ).getBody());
        verify(readService).getClients(adminId, branchId, query, pageable);
    }
}
