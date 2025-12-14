package kz.edu.soccerhub.admin.application;

import kz.edu.soccerhub.admin.application.dto.AdminCreateCoachInput;
import kz.edu.soccerhub.admin.application.dto.AdminCreateCoachOutput;
import kz.edu.soccerhub.common.dto.coach.CoachCreateCommand;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.CoachPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCoachService {

    private final CoachPort coachPort;
    private final AdminService adminService;
    private final AdminBranchService adminBranchService;

    @Transactional
    public AdminCreateCoachOutput createCoach(UUID adminId, AdminCreateCoachInput input) {
        CoachCreateCommand command = CoachCreateCommand.builder()
                .firstName(input.firstName())
                .lastName(input.lastName())
                .email(input.email())
                .phone(input.phone())
                .build();

        UUID coachId = coachPort.create(command);
        log.info("Admin [{}] creates coach: {}",
                adminId,
                coachId
        );

        return AdminCreateCoachOutput.builder()
                .coachId(coachId)
                .build();
    }

    @Transactional
    public void assignCoachToBranch(UUID adminId, UUID coachId, UUID branchId) {
        boolean isAdminHasAccessToBranch = adminBranchService.verifyAdminBelongsToBranch(adminId, branchId);
        if (!isAdminHasAccessToBranch) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }

        adminService.findById(adminId)
                        .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        coachPort.assignToBranch(coachId, branchId);
    }

    @Transactional
    public void unassignCoachFromBranch(UUID adminId, UUID coachId, UUID branchId) {
        boolean isAdminHasAccessToBranch = adminBranchService.verifyAdminBelongsToBranch(adminId, branchId);
        if (!isAdminHasAccessToBranch) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }

        adminService.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        coachPort.unassignFromBranch(coachId, branchId);
    }

    @Transactional(readOnly = true)
    public Page<CoachDto> getCoachByBranchId(UUID adminId, UUID branchId, Pageable pageable) {
        boolean isAdminHasAccessToBranch = adminBranchService.verifyAdminBelongsToBranch(adminId, branchId);
        if (!isAdminHasAccessToBranch) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }

        return coachPort.getCoaches(Set.of(branchId), pageable);
    }
}
