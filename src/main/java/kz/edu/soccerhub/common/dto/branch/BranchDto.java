package kz.edu.soccerhub.common.dto.branch;

import lombok.Builder;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record BranchDto(UUID id, String name, String address, UUID clubId, String timezone,
        boolean active, String phone, String email, UUID managerAdminId,
        List<BranchWorkingDay> workingHours, Instant archivedAt) {}
