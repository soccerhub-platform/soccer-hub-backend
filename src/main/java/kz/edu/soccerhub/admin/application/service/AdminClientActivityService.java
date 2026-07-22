package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.client.AdminClientActivityOutput;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.client.ClientActivityDto;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.ClientActivityPort;
import kz.edu.soccerhub.common.port.ClientWorkspacePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminClientActivityService {

    private final ClientActivityPort activityPort;
    private final ClientWorkspacePort clientWorkspacePort;
    private final AdminService adminService;
    private final AdminBranchService adminBranchService;

    @Transactional(readOnly = true)
    public Page<AdminClientActivityOutput> getActivity(UUID adminId, UUID clientId, Pageable pageable) {
        verifyAccess(adminId, clientWorkspacePort.getClientBranchId(clientId));
        return activityPort.getClientActivity(clientId, pageable).map(this::toOutput);
    }

    private AdminClientActivityOutput toOutput(ClientActivityDto activity) {
        UUID actorId = activity.actorUserId();
        return new AdminClientActivityOutput(activity.id(), activity.type(), activity.occurredAt(),
                actorId == null ? null : new AdminClientActivityOutput.ActorRef(actorId, actorName(actorId)),
                activity.payload() == null ? Map.of() : activity.payload());
    }

    private String actorName(UUID actorId) {
        return adminService.findById(actorId).map(this::fullName).orElse(null);
    }

    private String fullName(AdminDto admin) {
        String name = ((admin.firstName() == null ? "" : admin.firstName()) + " "
                + (admin.lastName() == null ? "" : admin.lastName())).trim();
        return name.isBlank() ? admin.email() : name;
    }

    private void verifyAccess(UUID adminId, UUID branchId) {
        adminService.findById(adminId).orElseThrow(() -> new NotFoundException("Admin not found", adminId));
        if (branchId == null || !adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }
    }
}
