package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.client.AdminClientCreateInput;
import kz.edu.soccerhub.admin.application.dto.client.AdminClientStatusInput;
import kz.edu.soccerhub.admin.application.dto.client.AdminClientUpdateInput;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientActivityType;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceDetailsOutput;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceStatusCommand;
import kz.edu.soccerhub.common.dto.client.ClientWorkspaceUpdateCommand;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.ClientWorkspacePort;
import kz.edu.soccerhub.common.port.ClientActivityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminClientWriteService {

    private final ClientWorkspacePort clientWorkspacePort;
    private final AdminService adminService;
    private final AdminBranchService adminBranchService;
    private final ClientActivityPort clientActivityPort;

    @Transactional
    public ClientWorkspaceDetailsOutput create(UUID adminId, AdminClientCreateInput input) {
        verifyAdminAccess(adminId, input.branchId());
        ClientWorkspaceDetailsOutput output = clientWorkspacePort.create(new ClientWorkspaceCreateCommand(
                input.branchId(), input.firstName(), input.lastName(), input.phone(), input.email(),
                input.source(), input.comments()
        ));
        clientActivityPort.recordClientActivity(output.client().id(), adminId, ClientActivityType.CLIENT_CREATED,
                Map.of("clientName", output.client().fullName()));
        return output;
    }

    @Transactional
    public ClientWorkspaceDetailsOutput update(UUID adminId, UUID clientId, AdminClientUpdateInput input) {
        verifyAdminAccess(adminId, clientWorkspacePort.getClientBranchId(clientId));
        ClientWorkspaceDetailsOutput output = clientWorkspacePort.update(new ClientWorkspaceUpdateCommand(
                clientId, input.firstName(), input.lastName(), input.phone(), input.email(), input.source(), input.comments()
        ));
        clientActivityPort.recordClientActivity(clientId, adminId, ClientActivityType.CLIENT_UPDATED,
                Map.of("clientName", output.client().fullName()));
        return output;
    }

    @Transactional
    public ClientWorkspaceDetailsOutput changeStatus(UUID adminId, UUID clientId, AdminClientStatusInput input) {
        verifyAdminAccess(adminId, clientWorkspacePort.getClientBranchId(clientId));
        ClientWorkspaceDetailsOutput output = clientWorkspacePort.changeStatus(new ClientWorkspaceStatusCommand(clientId, input.status()));
        clientActivityPort.recordClientActivity(clientId, adminId, ClientActivityType.CLIENT_STATUS_CHANGED,
                Map.of("status", output.client().status()));
        return output;
    }

    private void verifyAdminAccess(UUID adminId, UUID branchId) {
        adminService.findById(adminId).orElseThrow(() -> new NotFoundException("Admin not found", adminId));
        if (branchId == null || !adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }
    }
}
