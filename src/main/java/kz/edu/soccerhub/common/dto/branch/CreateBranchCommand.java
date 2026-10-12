package kz.edu.soccerhub.common.dto.branch;

import lombok.Builder;
import java.util.List;
import java.util.UUID;

@Builder
public record CreateBranchCommand(UUID clubId, String name, String address,
        String phone, String email, UUID managerAdminId, List<BranchWorkingDay> workingHours) {}
