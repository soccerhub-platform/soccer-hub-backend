package kz.edu.soccerhub.common.dto.analytics;

public record DashboardLeadWeeklyTrendItemOutput(
        String bucket,
        long newLeads,
        long wonLeads,
        long lostLeads
) {
}
