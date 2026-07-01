package kz.edu.soccerhub.admin.application.dto.dashboard;

import java.util.List;

public record AdminDashboardSeriesDto(
        String code,
        String label,
        String unit,
        List<AdminDashboardSeriesPointDto> points
) {
}
