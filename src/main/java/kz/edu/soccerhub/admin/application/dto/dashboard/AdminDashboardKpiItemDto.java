package kz.edu.soccerhub.admin.application.dto.dashboard;

public record AdminDashboardKpiItemDto(
        long value,
        String label,
        String hint
) {
}
