package kz.edu.soccerhub.admin.application.dto.dashboard;

public record AdminDashboardSummaryResponse(
        AdminDashboardMetaDto meta,
        AdminDashboardAlertsDto alerts,
        AdminDashboardKpisDto kpis,
        AdminDashboardBranchTodayDto branchSummary,
        AdminDashboardRisksDto risks,
        AdminDashboardLeadFunnelDto funnel,
        AdminDashboardTodayScheduleDto todaySchedule,
        AdminDashboardWeeklyTrendDto weeklyDynamics
) {
}
