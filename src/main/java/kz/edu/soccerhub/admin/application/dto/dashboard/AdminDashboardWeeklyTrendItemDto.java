package kz.edu.soccerhub.admin.application.dto.dashboard;

public record AdminDashboardWeeklyTrendItemDto(
        String bucket,
        long newLeads,
        long wonLeads,
        long lostLeads
) {
}
