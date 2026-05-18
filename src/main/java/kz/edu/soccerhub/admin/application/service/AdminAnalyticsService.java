package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.common.dto.analytics.AnalyticsCohortBy;
import kz.edu.soccerhub.common.dto.analytics.AnalyticsGroupBy;
import kz.edu.soccerhub.common.dto.analytics.CoachLoadAnalyticsOutput;
import kz.edu.soccerhub.common.dto.analytics.FunnelAnalyticsOutput;
import kz.edu.soccerhub.common.dto.analytics.RetentionAnalyticsOutput;
import kz.edu.soccerhub.common.dto.analytics.SlaAnalyticsOutput;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.AnalyticsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

    private final AnalyticsPort analyticsPort;
    private final AdminService adminService;
    private final AdminBranchService adminBranchService;

    @Transactional(readOnly = true)
    public FunnelAnalyticsOutput getFunnelAnalytics(
            UUID adminId,
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone
    ) {
        verifyAdminAccess(adminId, branchId);
        return analyticsPort.getFunnelAnalytics(branchId, dateFrom, dateTo, groupBy, timezone);
    }

    @Transactional(readOnly = true)
    public CoachLoadAnalyticsOutput getCoachLoadAnalytics(
            UUID adminId,
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone
    ) {
        verifyAdminAccess(adminId, branchId);
        return analyticsPort.getCoachLoadAnalytics(branchId, dateFrom, dateTo, groupBy, timezone);
    }

    @Transactional(readOnly = true)
    public RetentionAnalyticsOutput getRetentionAnalytics(
            UUID adminId,
            UUID branchId,
            AnalyticsCohortBy cohortBy,
            int periods,
            String timezone
    ) {
        verifyAdminAccess(adminId, branchId);
        return analyticsPort.getRetentionAnalytics(branchId, cohortBy, periods, timezone);
    }

    @Transactional(readOnly = true)
    public SlaAnalyticsOutput getSlaAnalytics(
            UUID adminId,
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone
    ) {
        verifyAdminAccess(adminId, branchId);
        return analyticsPort.getSlaAnalytics(branchId, dateFrom, dateTo, groupBy, timezone);
    }

    private void verifyAdminAccess(UUID adminId, UUID branchId) {
        adminService.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        boolean hasAccess = adminBranchService.verifyAdminBelongsToBranch(adminId, branchId);
        if (!hasAccess) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }
    }
}

