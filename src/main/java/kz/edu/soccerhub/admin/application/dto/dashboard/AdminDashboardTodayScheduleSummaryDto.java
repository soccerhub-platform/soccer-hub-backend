package kz.edu.soccerhub.admin.application.dto.dashboard;

public record AdminDashboardTodayScheduleSummaryDto(
        int total,
        int active,
        int cancelled
) {
}
