package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.domain.model.AdminBranch;
import kz.edu.soccerhub.admin.domain.model.AdminProfile;
import kz.edu.soccerhub.admin.domain.repository.AdminBranchRepository;
import kz.edu.soccerhub.common.port.BranchPort;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AdminBranchServiceTest {
    @Test
    void unassignRemovesChildFromManagedParentBeforeDeletion() {
        var repository = mock(AdminBranchRepository.class);
        var service = new AdminBranchService(repository, mock(BranchPort.class),
                mock(GroupPort.class), mock(CoachPort.class));
        var adminId = UUID.randomUUID();
        var branchId = UUID.randomUUID();
        var parent = AdminProfile.builder().id(adminId).build();
        var removed = AdminBranch.builder().id(UUID.randomUUID()).adminId(adminId)
                .branchId(branchId).admin(parent).build();
        var retained = AdminBranch.builder().id(UUID.randomUUID()).adminId(adminId)
                .branchId(UUID.randomUUID()).admin(parent).build();
        parent.setAdminBranches(new ArrayList<>(List.of(removed, retained)));
        when(repository.findAllByAdminId(adminId)).thenReturn(List.of(removed, retained));
        doAnswer(invocation -> {
            assertEquals(List.of(retained), parent.getAdminBranches());
            return null;
        }).when(repository).delete(removed);

        service.unassignFromBranch(adminId, branchId);

        verify(repository).delete(removed);
        verify(repository, never()).delete(retained);
        assertEquals(List.of(retained), parent.getAdminBranches());
    }
}
