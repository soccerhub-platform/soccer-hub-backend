package kz.edu.soccerhub.admin.application.dto.dashboard;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminDashboardMetaDto(
        UUID branchId,
        String branchName,
        LocalDate date,
        String timezone,
        OffsetDateTime generatedAt
) {
}
