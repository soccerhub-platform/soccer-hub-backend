package kz.edu.soccerhub.admin.application.dto.dashboard;

import java.util.List;

public record AdminDashboardSummaryResponse(
        AdminDashboardMetaDto meta,
        AdminDashboardHeroDto hero,
        List<AdminDashboardAttentionItemDto> attention,
        AdminDashboardKpisDto kpis,
        AdminDashboardBranchTodayDto branchToday,
        List<AdminDashboardRiskItemDto> risks,
        AdminDashboardLeadFunnelDto leadFunnel,
        AdminDashboardTodayScheduleDto todaySchedule,
        AdminDashboardWeeklyTrendDto weeklyTrend
) {
}
