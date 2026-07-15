package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.group.AdminGroupActivityOutput;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.group.GroupActivityDto;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.GroupActivityPort;
import kz.edu.soccerhub.common.port.GroupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminGroupActivityService {

    private final GroupActivityPort groupActivityPort;
    private final GroupPort groupPort;
    private final AdminService adminService;
    private final AdminBranchService adminBranchService;

    @Transactional(readOnly = true)
    public Page<AdminGroupActivityOutput> getGroupActivity(UUID adminId, UUID groupId, Pageable pageable) {
        verifyAdmin(adminId);
        GroupDto group = groupPort.getGroupById(groupId);
        verifyAdminBranchAccess(adminId, group.branchId());

        return groupActivityPort.getGroupActivity(groupId, pageable)
                .map(this::toOutput);
    }

    private AdminGroupActivityOutput toOutput(GroupActivityDto activity) {
        GroupActivityDto.ActorRef actor = activity.actor();
        return new AdminGroupActivityOutput(
                activity.id(),
                activity.type(),
                activity.occurredAt(),
                actor == null ? null : new AdminGroupActivityOutput.ActorRef(actor.id(), resolveActorName(actor.id())),
                activity.payload() == null ? Map.of() : activity.payload()
        );
    }

    private String resolveActorName(UUID actorUserId) {
        Optional<AdminDto> actor = adminService.findById(actorUserId);
        if (actor.isEmpty()) {
            return null;
        }
        String firstName = actor.get().firstName() == null ? "" : actor.get().firstName().trim();
        String lastName = actor.get().lastName() == null ? "" : actor.get().lastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        return actor.get().email();
    }

    private void verifyAdmin(UUID adminId) {
        adminService.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));
    }

    private void verifyAdminBranchAccess(UUID adminId, UUID branchId) {
        if (!adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }
    }
}
