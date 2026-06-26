package kz.edu.soccerhub.admin.application.dto.dashboard;

import java.util.List;

public record AdminDashboardKpisDto(
        List<AdminDashboardKpiItemDto> items
) {
}
