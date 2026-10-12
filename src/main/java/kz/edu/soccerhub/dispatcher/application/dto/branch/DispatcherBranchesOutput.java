package kz.edu.soccerhub.dispatcher.application.dto.branch;

import kz.edu.soccerhub.common.dto.branch.BranchWorkingDay;
import lombok.Builder;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record DispatcherBranchesOutput(UUID branchId, String name, String address, UUID clubId,
        boolean active, String phone, String email, UUID managerAdminId, String managerName,
        List<BranchWorkingDay> workingHours, Instant archivedAt) {}
