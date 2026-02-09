package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.coach.AdminCoachUpdateCoachStatusInput;
import kz.edu.soccerhub.admin.application.dto.coach.AdminCreateCoachInput;
import kz.edu.soccerhub.admin.application.dto.coach.AdminCreateCoachOutput;
import kz.edu.soccerhub.common.domain.enums.Role;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommand;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommandOutput;
import kz.edu.soccerhub.common.dto.coach.CoachCreateCommand;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.AuthPort;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.dispatcher.application.service.PasswordGenerator;
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
    private final AuthPort authPort;
    private final AdminService adminService;
    private final AdminBranchService adminBranchService;
    private final PasswordGenerator passwordGenerator;

    @Transactional
    public AdminCreateCoachOutput createCoach(UUID adminId, AdminCreateCoachInput input) {
        final String tempPassword = passwordGenerator.generate(6);

        AuthRegisterCommand authRegisterCommand = AuthRegisterCommand.builder()
                .email(input.email())
                .password(tempPassword)
                .roles(Set.of(Role.COACH))
                .requireToChangePassword(true)
                .build();

        AuthRegisterCommandOutput output = authPort.register(authRegisterCommand);

        CoachCreateCommand command = CoachCreateCommand.builder()
                .id(output.id())
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

        UUID branchId = input.branchId();
        if (branchId != null) {
            assignCoachToBranch(adminId, coachId, branchId);
            log.info("Admin {} assigned coach {} to the branch {}", adminId, coachId, branchId);
        }

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

    @Transactional
    public void updateCoachStatus(UUID adminId, UUID coachId, AdminCoachUpdateCoachStatusInput input) {
        adminService.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        switch (input.status()) {
            case ACTIVE -> coachPort.enableCoach(coachId);
            case INACTIVE -> coachPort.disableCoach(coachId);
            default -> throw new BadRequestException("Invalid coach status", input.status());
        }
    }
}
