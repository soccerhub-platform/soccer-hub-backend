package kz.edu.soccerhub.admin.application.dto.dashboard;

import java.util.List;

public record AdminDashboardWeeklyTrendDto(
        AdminDashboardWeeklyTrendPeriodDto period,
        List<AdminDashboardSeriesDto> series,
        boolean isEmpty,
        String emptyReason
) {
}
