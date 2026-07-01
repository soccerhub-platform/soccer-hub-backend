package kz.edu.soccerhub.admin.application.dto.dashboard;

import java.util.List;

public record AdminDashboardRisksDto(
        List<AdminDashboardRiskItemDto> items
) {
}
