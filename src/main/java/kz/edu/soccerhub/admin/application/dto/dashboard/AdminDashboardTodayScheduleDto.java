package kz.edu.soccerhub.admin.application.dto.dashboard;

import java.util.List;

public record AdminDashboardTodayScheduleDto(
        AdminDashboardTodayScheduleSummaryDto summary,
        AdminDashboardSessionDto nextSession,
        List<AdminDashboardSessionDto> items
) {
}
