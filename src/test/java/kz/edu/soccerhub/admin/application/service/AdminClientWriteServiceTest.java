package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.client.AdminClientCreateInput;
import kz.edu.soccerhub.admin.application.dto.client.AdminClientStatusInput;
import kz.edu.soccerhub.admin.application.dto.client.AdminClientUpdateInput;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceDetailsOutput;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.port.ClientWorkspacePort;
import kz.edu.soccerhub.common.port.ClientActivityPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminClientWriteServiceTest {

    @Mock private ClientWorkspacePort clientWorkspacePort;
    @Mock private AdminService adminService;
    @Mock private AdminBranchService adminBranchService;
    @Mock private ClientActivityPort clientActivityPort;

    private AdminClientWriteService service;

    @BeforeEach
    void setUp() {
        service = new AdminClientWriteService(clientWorkspacePort, adminService, adminBranchService, clientActivityPort);
    }

    @Test
    void commandsShouldVerifyBranchAndDelegateThroughPort() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        ClientWorkspaceDetailsOutput output = output(clientId, branchId);
        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(clientWorkspacePort.getClientBranchId(clientId)).thenReturn(branchId);
        when(clientWorkspacePort.create(any())).thenReturn(output);
        when(clientWorkspacePort.update(any())).thenReturn(output);
        when(clientWorkspacePort.changeStatus(any())).thenReturn(output);

        assertSame(output, service.create(adminId, new AdminClientCreateInput(
                branchId, "Roman", "Romanov", "+77001234567", "roman@example.com", null, null
        )));
        assertSame(output, service.update(adminId, clientId, new AdminClientUpdateInput(
                "Roman", "Romanov", "+77001234567", "roman@example.com", null, null
        )));
        assertSame(output, service.changeStatus(adminId, clientId, new AdminClientStatusInput("ACTIVE")));

        verify(clientWorkspacePort).create(any());
        verify(clientWorkspacePort).update(any());
        verify(clientWorkspacePort).changeStatus(any());
        verify(clientActivityPort, org.mockito.Mockito.times(3)).recordClientActivity(any(), any(), any(), any());
    }

    @Test
    void createShouldRejectBranchOutsideAdminScope() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> service.create(adminId, new AdminClientCreateInput(
                branchId, "Roman", null, null, null, null, null
        )));
        verify(clientWorkspacePort, never()).create(any());
    }

    private ClientWorkspaceDetailsOutput output(UUID clientId, UUID branchId) {
        return new ClientWorkspaceDetailsOutput(
                new ClientWorkspaceDetailsOutput.ClientBlock(
                        clientId, branchId, "Roman", "Roman", null, null, null,
                        "NEW", null, null, null
                ),
                new ClientWorkspaceDetailsOutput.SummaryBlock(0, 0, 0, null),
                List.of(),
                new ClientWorkspaceDetailsOutput.CapabilitiesBlock(true, true, true, false, true, false, true)
        );
    }
}
