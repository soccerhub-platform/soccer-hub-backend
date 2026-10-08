package kz.edu.soccerhub.dispatcher.application.service;

import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.dispatcher.application.dto.admin.DispatcherAdminChangeStatusInput;
import kz.edu.soccerhub.dispatcher.application.dto.admin.DispatcherAdminUpdateInput;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.mockito.Mockito.*;

class DispatcherAdminServiceTest {
    @Test
    void creatorCanUpdateAndDisableAdminWithoutBranches() {
        var adminPort = mock(AdminPort.class);
        var authPort = mock(AuthPort.class);
        var branches = mock(DispatcherBranchService.class);
        var service = new DispatcherAdminService(mock(PasswordGenerator.class), branches,
                mock(ClubPort.class), mock(BranchPort.class), authPort, adminPort);
        var dispatcherId = UUID.randomUUID();
        var adminId = UUID.randomUUID();
        when(adminPort.findById(adminId)).thenReturn(Optional.of(AdminDto.builder()
                .id(adminId).dispatcherId(dispatcherId).branchesId(Set.of()).build()));

        service.changeAdminStatus(dispatcherId, adminId, new DispatcherAdminChangeStatusInput(false));
        service.updateAdmin(dispatcherId, adminId, new DispatcherAdminUpdateInput("QA", "Admin", null));

        verify(adminPort).changeStatus(adminId, false);
        verify(authPort).disableUser(adminId);
        verify(adminPort).updateAdminInfo(eq(adminId), any());
        verifyNoInteractions(branches);
    }
}
