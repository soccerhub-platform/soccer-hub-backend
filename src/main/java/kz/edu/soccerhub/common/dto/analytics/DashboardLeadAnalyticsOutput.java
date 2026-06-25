package kz.edu.soccerhub.common.dto.analytics;

import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;

import java.util.List;
import java.util.Map;

public record DashboardLeadAnalyticsOutput(
        long newLeads,
        int avgFirstResponseMinutes,
        long slaBreachedLeads,
        Map<LeadStatus, Long> funnelTotals,
        List<DashboardLeadWeeklyTrendItemOutput> weeklyTrend
) {
}
