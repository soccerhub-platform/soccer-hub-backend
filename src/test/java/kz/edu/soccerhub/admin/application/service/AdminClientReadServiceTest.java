package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.client.ClientWorkspacePageOutput;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceListQuery;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.port.ClientWorkspacePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminClientReadServiceTest {

    @Mock private ClientWorkspacePort clientWorkspacePort;
    @Mock private AdminService adminService;
    @Mock private AdminBranchService adminBranchService;

    private AdminClientReadService service;

    @BeforeEach
    void setUp() {
        service = new AdminClientReadService(clientWorkspacePort, adminService, adminBranchService);
    }

    @Test
    void listShouldVerifyAccessAndDelegate() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);
        ClientWorkspaceListQuery query = new ClientWorkspaceListQuery("roman", Set.of("ACTIVE"), "ALL", "ALL", "PAID");
        ClientWorkspacePageOutput output = new ClientWorkspacePageOutput(
                List.of(), 0, 0, 0, 20,
                new ClientWorkspacePageOutput.Summary(0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, "KZT", false)
        );
        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(clientWorkspacePort.getClients(branchId, query, pageable)).thenReturn(output);

        assertSame(output, service.getClients(adminId, branchId, query, pageable));
        verify(clientWorkspacePort).getClients(branchId, query, pageable);
    }

    @Test
    void listShouldRejectUnavailableBranch() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        when(adminService.findById(adminId)).thenReturn(Optional.of(AdminDto.builder().id(adminId).build()));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> service.getClients(
                adminId, branchId, new ClientWorkspaceListQuery(null, Set.of(), null, null, null), PageRequest.of(0, 20)
        ));
        verify(clientWorkspacePort, never()).getClients(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
