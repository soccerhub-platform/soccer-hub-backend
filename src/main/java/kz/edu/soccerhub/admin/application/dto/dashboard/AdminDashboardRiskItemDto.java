package kz.edu.soccerhub.admin.application.dto.dashboard;

public record AdminDashboardRiskItemDto(
        String code,
        String label,
        String description,
        long value,
        String unit,
        String tone,
        String target
) {
}
