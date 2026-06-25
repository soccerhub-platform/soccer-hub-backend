package kz.edu.soccerhub.admin.application.dto.dashboard;

public record AdminDashboardKpisDto(
        AdminDashboardKpiItemDto newLeads,
        AdminDashboardKpiItemDto activeGroups,
        AdminDashboardKpiItemDto trainingsToday,
        AdminDashboardKpiItemDto paymentsToday
) {
}
