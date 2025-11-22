package kz.edu.soccerhub.admin.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.admin.domain.model.AdminProfileEntity;
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

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService implements AdminPort {

    private final AdminProfileRepository adminProfileRepository;
    private final BranchPort branchPort;

    @Override
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
                .branchId(command.branchId())
                .active(true)
                .build();

        AdminProfileEntity saved = adminProfileRepository.save(entity);
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
        return adminProfileRepository.findAllByBranchId(branchId)
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
    public void assignToBranch(UUID adminId, UUID uuid) {
        adminProfileRepository.findById(adminId)
                .map(adminProfile -> {
                    adminProfile.setBranchId(uuid);
                    return adminProfile;
                })
                .map(adminProfileRepository::save)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));
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
                .branchId(adminProfileEntity.getBranchId())
                .build();
    }
}
