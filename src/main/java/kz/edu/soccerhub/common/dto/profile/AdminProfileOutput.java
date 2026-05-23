package kz.edu.soccerhub.common.dto.profile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminProfileOutput(
        UUID adminId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String specialization,
        String status,
        List<BranchRef> branches,
        List<String> permissions,
        LocalDateTime createdAt
) {
}
