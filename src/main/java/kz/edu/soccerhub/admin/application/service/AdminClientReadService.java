package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.common.dto.client.ClientWorkspaceDetailsOutput;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceListQuery;
import kz.edu.soccerhub.common.dto.client.ClientWorkspacePageOutput;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.ClientWorkspacePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminClientReadService {

    private final ClientWorkspacePort clientWorkspacePort;
    private final AdminService adminService;
    private final AdminBranchService adminBranchService;

    @Transactional(readOnly = true)
    public ClientWorkspacePageOutput getClients(UUID adminId, UUID branchId, ClientWorkspaceListQuery query, Pageable pageable) {
        verifyAdminAccess(adminId, branchId);
        return clientWorkspacePort.getClients(branchId, query, pageable);
    }

    @Transactional(readOnly = true)
    public ClientWorkspaceDetailsOutput getClient(UUID adminId, UUID clientId) {
        verifyAdminAccess(adminId, clientWorkspacePort.getClientBranchId(clientId));
        return clientWorkspacePort.getClient(clientId);
    }

    private void verifyAdminAccess(UUID adminId, UUID branchId) {
        adminService.findById(adminId).orElseThrow(() -> new NotFoundException("Admin not found", adminId));
        if (branchId == null || !adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }
    }
}
