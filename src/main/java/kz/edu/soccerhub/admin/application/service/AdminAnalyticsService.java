package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.common.dto.analytics.AnalyticsGroupBy;
import kz.edu.soccerhub.common.dto.analytics.AnalyticsResponseOutput;
import kz.edu.soccerhub.common.exception.AnalyticsForbiddenBranchException;
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
    public AnalyticsResponseOutput getFunnelAnalytics(
            UUID adminId,
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone,
            UUID coachId,
            UUID groupId
    ) {
        verifyAdminAccess(adminId, branchId);
        return analyticsPort.getFunnelAnalytics(branchId, dateFrom, dateTo, groupBy, timezone, coachId, groupId);
    }

    @Transactional(readOnly = true)
    public AnalyticsResponseOutput getCoachLoadAnalytics(
            UUID adminId,
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone,
            UUID coachId,
            UUID groupId
    ) {
        verifyAdminAccess(adminId, branchId);
        return analyticsPort.getCoachLoadAnalytics(branchId, dateFrom, dateTo, groupBy, timezone, coachId, groupId);
    }

    @Transactional(readOnly = true)
    public AnalyticsResponseOutput getRetentionAnalytics(
            UUID adminId,
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone,
            UUID coachId,
            UUID groupId
    ) {
        verifyAdminAccess(adminId, branchId);
        return analyticsPort.getRetentionAnalytics(branchId, dateFrom, dateTo, groupBy, timezone, coachId, groupId);
    }

    @Transactional(readOnly = true)
    public AnalyticsResponseOutput getSlaAnalytics(
            UUID adminId,
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone,
            UUID coachId,
            UUID groupId
    ) {
        verifyAdminAccess(adminId, branchId);
        return analyticsPort.getSlaAnalytics(branchId, dateFrom, dateTo, groupBy, timezone, coachId, groupId);
    }

    @Transactional(readOnly = true)
    public AnalyticsResponseOutput getLossReasonsAnalytics(
            UUID adminId,
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone,
            UUID coachId,
            UUID groupId
    ) {
        verifyAdminAccess(adminId, branchId);
        return analyticsPort.getLossReasonsAnalytics(branchId, dateFrom, dateTo, groupBy, timezone, coachId, groupId);
    }

    @Transactional(readOnly = true)
    public AnalyticsResponseOutput getKpiAnalytics(
            UUID adminId,
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone,
            UUID coachId,
            UUID groupId
    ) {
        verifyAdminAccess(adminId, branchId);
        return analyticsPort.getKpiAnalytics(branchId, dateFrom, dateTo, groupBy, timezone, coachId, groupId);
    }

    private void verifyAdminAccess(UUID adminId, UUID branchId) {
        adminService.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found", adminId));

        boolean hasAccess = adminBranchService.verifyAdminBelongsToBranch(adminId, branchId);
        if (!hasAccess) {
            throw new AnalyticsForbiddenBranchException(branchId);
        }
    }
}
