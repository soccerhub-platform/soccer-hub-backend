package kz.edu.soccerhub.admin.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.admin.domain.model.AdminBranchesEntity;
import kz.edu.soccerhub.admin.domain.model.AdminProfileEntity;
import kz.edu.soccerhub.admin.domain.repository.AdminBranchRepository;
import kz.edu.soccerhub.admin.domain.repository.AdminProfileRepository;
import kz.edu.soccerhub.common.dto.admin.AdminCreateCommand;
import kz.edu.soccerhub.common.dto.admin.AdminCreateCommandOutput;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.admin.AdminUpdateCommand;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.common.port.BranchPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService implements AdminPort {

    private final AdminProfileRepository adminProfileRepository;
    private final BranchPort branchPort;
    private final AdminBranchRepository adminBranchRepository;

    @Override
    @Transactional
    public AdminCreateCommandOutput create(@Valid AdminCreateCommand command) {
        boolean isBranchExist = branchPort.isExist(command.branchId());
        if (!isBranchExist) {
            throw new NotFoundException("Branch not found", command.branchId());
        }

        AdminProfileEntity entity = AdminProfileEntity.builder()
                .id(command.userId())
                .firstName(command.firstName())
                .lastName(command.lastName())
                .email(command.email())
                .phone(command.phone())
                .active(true)
                .build();

        AdminProfileEntity saved = adminProfileRepository.save(entity);

        assignToBranch(saved.getId(), command.branchId());

        return new AdminCreateCommandOutput(saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdminDto> findById(UUID adminId) {
        return adminProfileRepository.findById(adminId)
                .map(this::toDto);

    }

    @Override
    @Transactional(readOnly = true)
    public Collection<AdminDto> findAllByBranchId(UUID branchId) {
        Collection<AdminBranchesEntity> allByBranchId = adminBranchRepository.findAllByBranchId(branchId);

        Set<UUID> adminIds = allByBranchId.stream()
                .map(AdminBranchesEntity::getAdminId)
                .collect(Collectors.toSet());

        return adminProfileRepository.findAllById(adminIds)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public Collection<AdminDto> findAllByDispatcherId(UUID dispatcherId) {
        return adminProfileRepository.findAllByCreatedBy(dispatcherId.toString())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void changeStatus(UUID adminId, boolean active) {
        adminProfileRepository.findById(adminId)
                .map(adminProfile -> {
                    adminProfile.setActive(active);
                    return adminProfile;
                })
                .map(adminProfileRepository::save)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));
    }

    @Override
    @Transactional
    public void assignToBranch(UUID adminId, UUID branchId) {
        adminProfileRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        if (adminBranchRepository.existsByAdminIdAndBranchId(adminId, branchId)) {
            return;
        }

        AdminBranchesEntity adminBranchesEntity = AdminBranchesEntity.builder()
                .adminId(adminId)
                .branchId(branchId)
                .build();

        adminBranchRepository.save(adminBranchesEntity);
    }

    @Override
    @Transactional
    public void unassignFromBranch(UUID adminId, UUID branchId) {
        AdminProfileEntity admin = adminProfileRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        admin.getAdminBranches()
                .removeIf(link -> link.getBranchId().equals(branchId));
        adminProfileRepository.save(admin);
    }

    @Override
    public void updateAdminInfo(UUID adminId, AdminUpdateCommand build) {
        adminProfileRepository.findById(adminId)
                .map(adminProfile -> {
                    if (build.firstName() != null) {
                        adminProfile.setFirstName(build.firstName());
                    }
                    if (build.lastName() != null) {
                        adminProfile.setLastName(build.lastName());
                    }
                    if (build.email() != null) {
                        adminProfile.setEmail(build.email());
                    }
                    if (build.phone() != null) {
                        adminProfile.setPhone(build.phone());
                    }
                    return adminProfile;
                })
                .map(adminProfileRepository::save)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));
    }

    private AdminDto toDto(@NotNull AdminProfileEntity adminProfileEntity) {
        return AdminDto.builder()
                .id(adminProfileEntity.getId())
                .firstName(adminProfileEntity.getFirstName())
                .lastName(adminProfileEntity.getLastName())
                .email(adminProfileEntity.getEmail())
                .phone(adminProfileEntity.getPhone())
                .isActive(adminProfileEntity.getActive())
                .dispatcherId(UUID.fromString(adminProfileEntity.getCreatedBy()))
                .branchesId(adminProfileEntity.getAdminBranches()
                        .stream()
                        .map(AdminBranchesEntity::getBranchId)
                        .collect(Collectors.toSet()))
                .build();
    }
}
