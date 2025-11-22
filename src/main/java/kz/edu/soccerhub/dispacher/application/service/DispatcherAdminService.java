package kz.edu.soccerhub.dispacher.application.service;

import jakarta.validation.Valid;
import kz.edu.soccerhub.common.domain.enums.Role;
import kz.edu.soccerhub.common.dto.admin.AdminCreateCommand;
import kz.edu.soccerhub.common.dto.admin.AdminCreateCommandOutput;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.admin.AdminUpdateCommand;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommand;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommandOutput;
import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.dto.club.ClubDto;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.common.port.AuthPort;
import kz.edu.soccerhub.common.port.BranchPort;
import kz.edu.soccerhub.common.port.ClubPort;
import kz.edu.soccerhub.dispacher.application.dto.admin.*;
import kz.edu.soccerhub.dispacher.application.dto.branch.DispatcherBranchesOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatcherAdminService {

    private final PasswordGenerator passwordGenerator;
    private final DispatcherBranchService dispatcherBranchService;
    private final ClubPort clubPort;
    private final BranchPort branchPort;
    private final AuthPort authPort;
    private final AdminPort adminPort;

    @Transactional
    public DispatcherAdminRegisterOutput registerAdmin(DispatcherAdminRegisterInput input) {
        String tempPassword = passwordGenerator.generate(6);

        AuthRegisterCommand authRegisterCommand = AuthRegisterCommand.builder()
                .email(input.email())
                .password(tempPassword)
                .roles(Set.of(Role.ADMIN))
                .build();

        AuthRegisterCommandOutput authRegisterCommandOutput = authPort.register(authRegisterCommand);

        AdminCreateCommand adminCommand = AdminCreateCommand.builder()
                .userId(authRegisterCommandOutput.id())
                .firstName(input.firstName())
                .lastName(input.lastName())
                .phone(input.phone())
                .branchId(input.assignedBranch())
                .build();

        AdminCreateCommandOutput adminResult = adminPort.create(adminCommand);

        return new DispatcherAdminRegisterOutput(
                adminResult.adminId(),
                tempPassword
        );
    }


    public List<DispatcherAdminsOutput> getAdmins(UUID dispatcherId) {
        List<DispatcherBranchesOutput> dispatcherBranches = dispatcherBranchService.getDispatcherBranches(dispatcherId);

        Map<UUID, BranchDto> branchMap = branchPort.findAllByIds(
                dispatcherBranches.stream()
                        .map(DispatcherBranchesOutput::branchId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(BranchDto::id, b -> b));

        Map<UUID, ClubDto> clubMap = clubPort.findAllByIds(
                dispatcherBranches.stream()
                        .map(DispatcherBranchesOutput::clubId)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ClubDto::id, c -> c));

        return dispatcherBranches.stream()
                .map(DispatcherBranchesOutput::branchId)
                .flatMap(branchId -> adminPort.findAllByBranchId(branchId).stream())
                .map(admin -> {

                    BranchDto branchDto = branchMap.get(admin.branchId());
                    ClubDto clubDto = clubMap.get(branchDto.clubId());



                    return DispatcherAdminsOutput.builder()
                            .id(admin.id())
                            .firstName(admin.firstName())
                            .lastName(admin.lastName())
                            .email(admin.email())
                            .phone(admin.phone())
                            .isActive(admin.isActive())
                            .branch(DispatcherAdminsOutput.Branch.builder()
                                    .id(branchDto.id())
                                    .name(branchDto.name())
                                    .build())
                            .club(DispatcherAdminsOutput.Club.builder()
                                    .id(clubDto.id())
                                    .name(clubDto.name())
                                    .build())
                            .build();
                })
                .toList();
    }

    @Transactional
    public void deleteAdmin(UUID dispatcherId, UUID adminId) {
        AdminDto admin = adminPort.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        boolean isBranchBelongsToDispatcher = dispatcherBranchService.verifyBranchBelongsToDispatcher(dispatcherId, admin.branchId());
        if (!isBranchBelongsToDispatcher) {
            throw new BadRequestException("Dispatcher does not have access to admin", adminId);
        }

        authPort.delete(admin.id());
    }

    @Transactional
    public void changeAdminStatus(UUID dispatcherId, UUID adminId, @Valid DispatcherAdminChangeStatusInput input) {
        AdminDto admin = adminPort.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        boolean isBranchBelongsToDispatcher = dispatcherBranchService.verifyBranchBelongsToDispatcher(dispatcherId, admin.branchId());
        if (!isBranchBelongsToDispatcher) {
            throw new BadRequestException("Dispatcher does not have access to admin", adminId);
        }

        adminPort.changeStatus(adminId, input.active());
    }

    @Transactional
    public void assignAdminToBranch(UUID dispatcherId, UUID adminId, @Valid DispatcherAdminAssignBranchInput input) {
        AdminDto admin = adminPort.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        if (admin.branchId() != null && admin.branchId().equals(input.branchId())) {
            return;
        }

        boolean isNewBranchBelongsToDispatcher = dispatcherBranchService.verifyBranchBelongsToDispatcher(dispatcherId, input.branchId());
        if (!isNewBranchBelongsToDispatcher) {
            throw new BadRequestException("Dispatcher does not have access to branch", input.branchId());
        }

        adminPort.assignToBranch(adminId, input.branchId());
    }

    @Transactional
    public void updateAdmin(UUID dispatcherId, UUID adminId, @Valid DispatcherAdminUpdateInput input) {
        AdminDto admin = adminPort.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        boolean isBranchBelongsToDispatcher = dispatcherBranchService.verifyBranchBelongsToDispatcher(dispatcherId, admin.branchId());
        if (!isBranchBelongsToDispatcher) {
            throw new BadRequestException("Dispatcher does not have access to admin", adminId);
        }

        adminPort.updateAdminInfo(adminId, AdminUpdateCommand.builder()
                        .firstName(input.firstName())
                        .lastName(input.lastName())
                        .phone(input.phone())
                .build());
    }
}
