package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.branch.AdminBranchesOutput;
import kz.edu.soccerhub.admin.domain.model.AdminProfile;
import kz.edu.soccerhub.admin.domain.repository.AdminProfileRepository;
import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.dto.profile.AdminProfileOutput;
import kz.edu.soccerhub.common.dto.profile.BranchRef;
import kz.edu.soccerhub.common.dto.profile.ProfileUpdateInput;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.AuthPort;
import kz.edu.soccerhub.common.port.BranchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminProfileService {

    private final AdminProfileRepository adminProfileRepository;
    private final AdminBranchService adminBranchService;
    private final BranchPort branchPort;
    private final AuthPort authPort;

    @Transactional(readOnly = true)
    public AdminProfileOutput getProfile(UUID adminId) {
        AdminProfile profile = getAdmin(adminId);
        return toOutput(profile);
    }

    @Transactional
    public AdminProfileOutput updateProfile(UUID adminId, ProfileUpdateInput input) {
        AdminProfile profile = getAdmin(adminId);

        profile.setFirstName(input.firstName().trim());
        profile.setLastName(input.lastName().trim());
        profile.setEmail(input.email().trim().toLowerCase());
        profile.setPhone(input.phone().trim());
        profile.setSpecialization(trimToNull(input.specialization()));
        authPort.updateEmail(adminId, profile.getEmail());

        return toOutput(profile);
    }

    private AdminProfile getAdmin(UUID adminId) {
        return adminProfileRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));
    }

    private AdminProfileOutput toOutput(AdminProfile profile) {
        List<UUID> branchIds = adminBranchService.getAdminBranches(profile.getId()).stream()
                .map(AdminBranchesOutput::branchId)
                .toList();
        List<BranchRef> branches = branchPort.findAllByIds(branchIds).stream()
                .map(this::toBranchRef)
                .toList();

        return new AdminProfileOutput(
                profile.getId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getEmail() == null ? authPort.getEmail(profile.getId()) : profile.getEmail(),
                profile.getPhone(),
                profile.getSpecialization(),
                Boolean.FALSE.equals(profile.getActive()) ? "INACTIVE" : "ACTIVE",
                branches,
                List.of("LEADS", "CLIENTS", "GROUPS", "COACHES", "ANALYTICS"),
                profile.getCreatedAt()
        );
    }

    private BranchRef toBranchRef(BranchDto branch) {
        return new BranchRef(branch.id(), branch.name());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
