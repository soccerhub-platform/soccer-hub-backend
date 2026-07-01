package kz.edu.soccerhub.admin.application.dto.dashboard;

public record AdminDashboardKpiDeltaDto(
        long value,
        String unit,
        String tone,
        String label
) {
}
