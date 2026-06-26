package kz.edu.soccerhub.admin.application.dto.dashboard;

import java.util.List;

public record AdminDashboardAlertCardDto(
        String id,
        String tone,
        String icon,
        String title,
        String description,
        AdminDashboardActionDto action,
        List<AdminDashboardAlertCardDetailDto> details
) {
}
