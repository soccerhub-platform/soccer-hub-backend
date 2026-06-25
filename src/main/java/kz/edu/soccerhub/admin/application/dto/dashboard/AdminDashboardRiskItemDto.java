package kz.edu.soccerhub.admin.application.dto.dashboard;

public record AdminDashboardRiskItemDto(
        String code,
        String label,
        long value,
        String tone
) {
}
