package kz.edu.soccerhub.admin.application;

import kz.edu.soccerhub.admin.domain.model.AdminBranch;
import kz.edu.soccerhub.admin.domain.repository.AdminBranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBranchService {

    private final AdminBranchRepository adminBranchRepository;

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
    public Collection<AdminBranch> getAllByBranch(UUID branchId) {
        return adminBranchRepository.findAllByBranchId(branchId);
    }

    @Transactional(readOnly = true)
    public boolean verifyAdminBelongsToBranch(UUID adminId, UUID branchId) {
        return adminBranchRepository.existsByAdminIdAndBranchId(adminId, branchId);
    }
}
