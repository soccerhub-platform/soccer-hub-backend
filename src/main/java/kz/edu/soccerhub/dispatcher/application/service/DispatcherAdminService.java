package kz.edu.soccerhub.dispatcher.application.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
import kz.edu.soccerhub.dispatcher.application.dto.admin.*;
import kz.edu.soccerhub.dispatcher.application.dto.branch.DispatcherBranchesOutput;
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
                .requireToChangePassword(true)
                .build();

        AuthRegisterCommandOutput authRegisterCommandOutput = authPort.register(authRegisterCommand);

        AdminCreateCommand adminCommand = AdminCreateCommand.builder()
                .userId(authRegisterCommandOutput.id())
                .firstName(input.firstName())
                .email(authRegisterCommandOutput.email())
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

    @Transactional
    public DispatcherAdminResetPasswordOutput resetAdminPassword(UUID dispatcherId, UUID adminId) {
        AdminDto admin = adminPort.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        boolean hasAccess = isAdminCreatedByDispatcher(admin, dispatcherId);
        if (!hasAccess) {
            throw new BadRequestException("Dispatcher does not have access to admin", adminId);
        }
        String tempPassword = passwordGenerator.generate(8);
        authPort.resetPassword(adminId, tempPassword);

        log.info("Admin password reset: adminId={}, dispatcherId={}", adminId, dispatcherId);
        return new DispatcherAdminResetPasswordOutput(tempPassword);
    }


    public List<DispatcherAdminsOutput> getAdmins(UUID dispatcherId) {

        // 0. Загружаем всех админов этого диспетчера
        Collection<AdminDto> dispatcherAdmins = adminPort.findAllByDispatcherId(dispatcherId);

        // 1. Загружаем все ветки этого диспетчера
        List<DispatcherBranchesOutput> dispatcherBranches =
                dispatcherBranchService.getDispatcherBranches(dispatcherId);

        Set<UUID> dispatcherBranchIds = dispatcherBranches.stream()
                .map(DispatcherBranchesOutput::branchId)
                .collect(Collectors.toSet());

        Set<UUID> dispatcherClubIds = dispatcherBranches.stream()
                .map(DispatcherBranchesOutput::clubId)
                .collect(Collectors.toSet());

        // 2. Lookup-таблицы
        Map<UUID, BranchDto> branchMap = branchPort.findAllByIds(dispatcherBranchIds)
                .stream()
                .collect(Collectors.toMap(BranchDto::id, b -> b));

        Map<UUID, ClubDto> clubMap = clubPort.findAllByIds(dispatcherClubIds)
                .stream()
                .collect(Collectors.toMap(ClubDto::id, c -> c));

        // 3. Собираем финальный результат
        return dispatcherAdmins.stream()
                .map(admin -> {

                    // Берем только ветки админа, которые принадлежат этому диспетчеру
                    Set<DispatcherAdminsOutput.BranchWithClub> branchWithClubs =
                            admin.branchesId().stream()
                                    .filter(dispatcherBranchIds::contains) // ВАЖНО!
                                    .map(branchMap::get)
                                    .filter(Objects::nonNull)
                                    .map(branch -> {

                                        ClubDto club = clubMap.get(branch.clubId());

                                        return DispatcherAdminsOutput.BranchWithClub.builder()
                                                .branchId(branch.id())
                                                .branchName(branch.name())
                                                .clubId(club != null ? club.id() : null)
                                                .clubName(club != null ? club.name() : null)
                                                .build();
                                    })
                                    .collect(Collectors.toSet());

                    // Возвращаем админа даже если веток нет
                    return DispatcherAdminsOutput.builder()
                            .id(admin.id())
                            .firstName(admin.firstName())
                            .lastName(admin.lastName())
                            .email(admin.email())
                            .phone(admin.phone())
                            .isActive(admin.isActive())
                            .branches(branchWithClubs)
                            .build();
                })
                .toList();
    }

    @Transactional
    public void deleteAdmin(UUID dispatcherId, UUID adminId) {
        AdminDto admin = adminPort.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        if (!isAdminCreatedByDispatcher(admin, dispatcherId)) {
            throw new BadRequestException("Admin is not created by dispatcher", adminId);
        }

        authPort.delete(admin.id());
    }

    private boolean isAdminCreatedByDispatcher(@NotNull AdminDto admin, @NotNull UUID dispatcherId) {
        if (dispatcherId == null) {
            return false;
        }
        return dispatcherId.equals(admin.dispatcherId());
    }

    @Transactional
    public void changeAdminStatus(UUID dispatcherId, UUID adminId, @Valid DispatcherAdminChangeStatusInput input) {
        AdminDto admin = adminPort.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        boolean isBranchBelongsToDispatcher = dispatcherBranchService.verifyBranchBelongsToDispatcher(dispatcherId, admin.branchesId());
        if (!isBranchBelongsToDispatcher) {
            throw new BadRequestException("Dispatcher does not have access to admin", adminId);
        }

        adminPort.changeStatus(adminId, input.active());

        if (!input.active()) {
            authPort.disableUser(adminId);
        } else {
            authPort.enableUser(adminId);
        }
    }

    @Transactional
    public void assignAdminToBranch(UUID dispatcherId, UUID adminId, @Valid DispatcherAdminAssignBranchInput input) {
        AdminDto admin = adminPort.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        if (admin.branchesId() != null && admin.branchesId().contains(input.branchId())) {
            return;
        }

        boolean isNewBranchBelongsToDispatcher = dispatcherBranchService.verifyBranchBelongsToDispatcher(dispatcherId, input.branchId());
        if (!isNewBranchBelongsToDispatcher) {
            throw new BadRequestException("Dispatcher does not have access to branch", input.branchId());
        }

        adminPort.assignToBranch(adminId, input.branchId());
    }

    @Transactional
    public void unassignAdminFromBranch(UUID dispatcherId, UUID adminId, @Valid DispatcherAdminUnAssignBranchInput input) {
        adminPort.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        boolean isBranchBelongsToDispatcher = dispatcherBranchService.verifyBranchBelongsToDispatcher(dispatcherId, input.branchId());
        if (!isBranchBelongsToDispatcher) {
            throw new BadRequestException("Dispatcher does not have access to branch", input.branchId());
        }

        adminPort.unassignFromBranch(adminId, input.branchId());
    }

    @Transactional
    public void updateAdmin(UUID dispatcherId, UUID adminId, @Valid DispatcherAdminUpdateInput input) {
        AdminDto admin = adminPort.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        boolean isBranchBelongsToDispatcher = dispatcherBranchService.verifyBranchBelongsToDispatcher(dispatcherId, admin.branchesId());
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
