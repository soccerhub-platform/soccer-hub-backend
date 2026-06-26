package kz.edu.soccerhub.admin.api;

import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardAlertsDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardBranchTodayDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardKpiDeltaDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardKpiItemDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardKpisDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardLeadFunnelDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardLeadFunnelRowDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardMetaDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardRiskItemDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardRisksDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardSeriesDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardSeriesPointDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardSessionDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardSummaryResponse;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardTodayScheduleDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardTodayScheduleSummaryDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardWeeklyTrendDto;
import kz.edu.soccerhub.admin.application.dto.dashboard.AdminDashboardWeeklyTrendPeriodDto;
import kz.edu.soccerhub.admin.application.service.AdminDashboardSummaryService;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDashboardControllerTest {

    private final AdminDashboardSummaryService summaryService = Mockito.mock(AdminDashboardSummaryService.class);

    private AdminDashboardController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminDashboardController(summaryService);
    }

    @Test
    void shouldReturnSummarySectionsFromDedicatedEndpoints() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 26);
        String timezone = "Asia/Almaty";

        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(adminId.toString());
        when(jwt.getClaimAsStringList("roles")).thenReturn(List.of("ADMIN", "SUPER_ADMIN"));

        AdminDashboardAlertsDto alerts = new AdminDashboardAlertsDto(List.of(), List.of());
        AdminDashboardKpisDto kpis = new AdminDashboardKpisDto(List.of(
                new AdminDashboardKpiItemDto("newLeads", "Лиды", 12, "12", new AdminDashboardKpiDeltaDto(2, "count", "success", "+2"), "За сегодня", "/admin/leads", null, null),
                new AdminDashboardKpiItemDto("activeGroups", "Группы", 24, "24", new AdminDashboardKpiDeltaDto(0, "count", "info", "0"), "Активные", "/admin/groups", null, null),
                new AdminDashboardKpiItemDto("trainingsToday", "Тренировки", 14, "14", new AdminDashboardKpiDeltaDto(1, "count", "success", "+1"), "По расписанию", "/admin/dashboard/today-schedule", null, null),
                new AdminDashboardKpiItemDto("paymentsToday", "Оплаты", 1, "1", new AdminDashboardKpiDeltaDto(8500, "amount", "success", "+8500"), "KZT", "/admin/payments", 1L, java.math.BigDecimal.valueOf(18500))
        ));
        AdminDashboardBranchTodayDto branchSummary = new AdminDashboardBranchTodayDto(
                158L,
                6L,
                13L,
                14L,
                92,
                5L,
                2L,
                6,
                1,
                0,
                18,
                java.util.Map.of()
        );
        AdminDashboardRisksDto risks = new AdminDashboardRisksDto(List.of(
                new AdminDashboardRiskItemDto(
                        "low-attendance",
                        "Низкая посещаемость",
                        "Средняя посещаемость: 62%",
                        62,
                        "percent",
                        "danger",
                        "/admin/groups/" + UUID.randomUUID()
                )
        ));
        AdminDashboardLeadFunnelDto funnel = new AdminDashboardLeadFunnelDto(
                List.of(new AdminDashboardLeadFunnelRowDto(LeadStatus.NEW, "Новый", 5, 100)),
                20
        );
        AdminDashboardTodayScheduleDto todaySchedule = new AdminDashboardTodayScheduleDto(
                new AdminDashboardTodayScheduleSummaryDto(3, 2, 1),
                new AdminDashboardSessionDto(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "U-10",
                        UUID.randomUUID(),
                        "Coach",
                        OffsetDateTime.parse("2026-06-26T09:00:00+05:00"),
                        OffsetDateTime.parse("2026-06-26T10:00:00+05:00"),
                        "scheduled",
                        "regular"
                ),
                List.of()
        );
        AdminDashboardWeeklyTrendDto weeklyDynamics = new AdminDashboardWeeklyTrendDto(
                new AdminDashboardWeeklyTrendPeriodDto(date.minusDays(6), date),
                List.of(new AdminDashboardSeriesDto(
                        "leads",
                        "Лиды",
                        "count",
                        List.of(new AdminDashboardSeriesPointDto(date, java.math.BigDecimal.valueOf(8)))
                )),
                false,
                null
        );
        AdminDashboardSummaryResponse summary = new AdminDashboardSummaryResponse(
                new AdminDashboardMetaDto(branchId, "Branch", date, timezone, OffsetDateTime.parse("2026-06-26T12:00:00+05:00")),
                alerts,
                kpis,
                branchSummary,
                risks,
                funnel,
                todaySchedule,
                weeklyDynamics
        );

        when(summaryService.getSummary(eq(adminId), eq(branchId), eq(date), eq(timezone), eq(true))).thenReturn(summary);

        assertSame(summary, controller.getSummary(jwt, branchId, date, timezone).getBody());
        assertSame(alerts, controller.getAlerts(jwt, branchId, date, timezone).getBody());
        assertSame(kpis, controller.getKpis(jwt, branchId, date, timezone).getBody());
        when(summaryService.getBranchSummary(eq(adminId), eq(branchId), eq(date), eq(timezone), eq(true))).thenReturn(branchSummary);

        assertSame(branchSummary, controller.getBranchSummary(jwt, branchId, date, timezone).getBody());
        when(summaryService.getFunnel(eq(adminId), eq(branchId), eq(null), eq(null), eq(date), eq(timezone), eq(true))).thenReturn(funnel);

        assertSame(funnel, controller.getFunnel(jwt, branchId, null, null, date, timezone).getBody());
        when(summaryService.getTodaySchedule(eq(adminId), eq(branchId), eq(date), eq(timezone), eq(true))).thenReturn(todaySchedule);

        when(summaryService.getRisks(eq(adminId), eq(branchId), eq(date), eq(timezone), eq(true))).thenReturn(risks);

        assertSame(risks, controller.getRisks(jwt, branchId, date, timezone).getBody());
        assertSame(todaySchedule, controller.getTodaySchedule(jwt, branchId, date, timezone).getBody());
        when(summaryService.getWeeklyDynamics(eq(adminId), eq(branchId), eq(null), eq(null), eq(date), eq(timezone), eq(true))).thenReturn(weeklyDynamics);

        assertSame(weeklyDynamics, controller.getWeeklyDynamics(jwt, branchId, null, null, date, timezone).getBody());

        verify(summaryService, Mockito.times(3)).getSummary(eq(adminId), eq(branchId), eq(date), eq(timezone), eq(true));
        verify(summaryService).getBranchSummary(eq(adminId), eq(branchId), eq(date), eq(timezone), eq(true));
        verify(summaryService).getFunnel(eq(adminId), eq(branchId), eq(null), eq(null), eq(date), eq(timezone), eq(true));
        verify(summaryService).getRisks(eq(adminId), eq(branchId), eq(date), eq(timezone), eq(true));
        verify(summaryService).getTodaySchedule(eq(adminId), eq(branchId), eq(date), eq(timezone), eq(true));
        verify(summaryService).getWeeklyDynamics(eq(adminId), eq(branchId), eq(null), eq(null), eq(date), eq(timezone), eq(true));
    }

    @Test
    void shouldTreatMissingRolesAsNonSuperAdmin() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 26);

        Jwt jwt = Mockito.mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(adminId.toString());
        when(jwt.getClaimAsStringList("roles")).thenReturn(null);

        AdminDashboardSummaryResponse summary = new AdminDashboardSummaryResponse(
                new AdminDashboardMetaDto(branchId, "Branch", date, "Asia/Almaty", OffsetDateTime.parse("2026-06-26T12:00:00+05:00")),
                new AdminDashboardAlertsDto(List.of(), List.of()),
                new AdminDashboardKpisDto(List.of(
                        new AdminDashboardKpiItemDto("newLeads", "Лиды", 0, "0", new AdminDashboardKpiDeltaDto(0, "count", "info", "0"), null, "/admin/leads", null, null),
                        new AdminDashboardKpiItemDto("activeGroups", "Группы", 0, "0", new AdminDashboardKpiDeltaDto(0, "count", "info", "0"), null, "/admin/groups", null, null),
                        new AdminDashboardKpiItemDto("trainingsToday", "Тренировки", 0, "0", new AdminDashboardKpiDeltaDto(0, "count", "info", "0"), null, "/admin/dashboard/today-schedule", null, null),
                        new AdminDashboardKpiItemDto("paymentsToday", "Оплаты", 0, "0", new AdminDashboardKpiDeltaDto(0, "amount", "info", "0"), null, "/admin/payments", 0L, java.math.BigDecimal.ZERO)
                )),
                new AdminDashboardBranchTodayDto(0L, 0L, null, 0L, null, 0L, 0L, 0, 0, 0, 0, java.util.Map.of()),
                new AdminDashboardRisksDto(List.of()),
                new AdminDashboardLeadFunnelDto(List.of(), 0),
                new AdminDashboardTodayScheduleDto(new AdminDashboardTodayScheduleSummaryDto(0, 0, 0), null, List.of()),
                new AdminDashboardWeeklyTrendDto(new AdminDashboardWeeklyTrendPeriodDto(date.minusDays(6), date), List.of(), true, "empty")
        );

        when(summaryService.getSummary(eq(adminId), eq(branchId), eq(date), eq(null), eq(false))).thenReturn(summary);

        assertEquals(summary, controller.getSummary(jwt, branchId, date, null).getBody());
        verify(summaryService).getSummary(eq(adminId), eq(branchId), eq(date), eq(null), eq(false));
    }
}
