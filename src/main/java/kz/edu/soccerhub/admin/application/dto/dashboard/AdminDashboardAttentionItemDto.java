package kz.edu.soccerhub.admin.application.dto.dashboard;

public record AdminDashboardAttentionItemDto(
        String id,
        String tone,
        String area,
        String title,
        String description,
        AdminDashboardActionDto action
) {
}
