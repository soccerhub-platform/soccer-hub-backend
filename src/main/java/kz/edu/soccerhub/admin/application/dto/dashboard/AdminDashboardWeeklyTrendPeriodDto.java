package kz.edu.soccerhub.admin.application.dto.dashboard;

import java.time.LocalDate;

public record AdminDashboardWeeklyTrendPeriodDto(
        LocalDate from,
        LocalDate to
) {
}
