package kz.edu.soccerhub.admin.application.service;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.admin.application.dto.branch.AdminBranchesOutput;
import kz.edu.soccerhub.admin.domain.model.AdminBranch;
import kz.edu.soccerhub.admin.domain.repository.AdminBranchRepository;
import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.port.BranchPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBranchService {

    private final AdminBranchRepository adminBranchRepository;
    private final BranchPort branchPort;

    @Transactional
    public void assignToBranch(UUID adminId, UUID branchId) {
        if (verifyAdminBelongsToBranch(adminId, branchId)) {
            return;
        }

        AdminBranch adminBranchesEntity = AdminBranch.builder()
                .adminId(adminId)
                .branchId(branchId)
                .build();

        adminBranchRepository.save(adminBranchesEntity);
    }

    @Transactional
    public void unassignFromBranch(UUID adminId, UUID branchId) {
        adminBranchRepository.findAllByAdminId(adminId).stream()
                .filter(adminBranch -> adminBranch.getBranchId().equals(branchId))
                .findFirst()
                .ifPresent(adminBranchRepository::delete);
    }

    @Transactional(readOnly = true)
    public Collection<AdminBranchesOutput> getAdminBranches(@NotNull UUID adminId) {
        Set<UUID> adminBranchIds = adminBranchRepository.findAllByAdminId(adminId).stream()
                .map(AdminBranch::getBranchId)
                .collect(Collectors.toSet());
        Collection<BranchDto> branches = branchPort.findAllByIds(adminBranchIds);
        return branches.stream()
                .map(branch -> AdminBranchesOutput.builder()
                        .branchId(branch.id())
                        .name(branch.name())
                        .address(branch.address())
                        .clubId(branch.clubId())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Transactional(readOnly = true)
    public Collection<AdminBranch> getAllByBranch(UUID branchId) {
        return adminBranchRepository.findAllByBranchId(branchId);
    }

    @Transactional(readOnly = true)
    public boolean verifyAdminBelongsToBranch(UUID adminId, UUID branchId) {
        return adminBranchRepository.existsByAdminIdAndBranchId(adminId, branchId);
    }
}
