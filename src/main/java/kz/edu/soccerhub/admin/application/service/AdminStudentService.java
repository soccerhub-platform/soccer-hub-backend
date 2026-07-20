package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.student.AdminStudentDetailsOutput;
import kz.edu.soccerhub.admin.application.dto.student.AdminStudentUpdateInput;
import kz.edu.soccerhub.common.dto.student.StudentProfileDto;
import kz.edu.soccerhub.common.dto.student.StudentUpdateCommand;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.ClientPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminStudentService {

    private final ClientPort clientPort;
    private final AdminService adminService;
    private final AdminBranchService adminBranchService;
    private final AdminStudentReadService adminStudentReadService;

    @Transactional
    public AdminStudentDetailsOutput update(UUID adminId, UUID playerId, AdminStudentUpdateInput input) {
        StudentProfileDto profile = clientPort.getStudentProfile(playerId);
        verifyAdminAccess(adminId, profile.branchId());

        clientPort.updateStudent(playerId, new StudentUpdateCommand(
                input.firstName().trim(),
                input.lastName().trim(),
                input.birthDate(),
                trimToNull(input.position())
        ));

        return adminStudentReadService.getStudent(adminId, playerId);
    }

    private void verifyAdminAccess(UUID adminId, UUID branchId) {
        adminService.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));
        if (branchId == null || !adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
