package kz.edu.soccerhub.dispatcher.application.service;

import kz.edu.soccerhub.common.dto.analytics.AnalyticsCohortBy;
import kz.edu.soccerhub.common.dto.analytics.AnalyticsGroupBy;
import kz.edu.soccerhub.common.dto.analytics.CoachLoadAnalyticsOutput;
import kz.edu.soccerhub.common.dto.analytics.FunnelAnalyticsOutput;
import kz.edu.soccerhub.common.dto.analytics.RetentionAnalyticsOutput;
import kz.edu.soccerhub.common.dto.analytics.SlaAnalyticsOutput;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.port.AnalyticsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DispatcherAnalyticsService {

    private final AnalyticsPort analyticsPort;
    private final DispatcherBranchService dispatcherBranchService;

    @Transactional(readOnly = true)
    public FunnelAnalyticsOutput getFunnelAnalytics(
            UUID dispatcherId,
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone
    ) {
        verifyDispatcherAccess(dispatcherId, branchId);
        return analyticsPort.getFunnelAnalytics(branchId, dateFrom, dateTo, groupBy, timezone);
    }

    @Transactional(readOnly = true)
    public CoachLoadAnalyticsOutput getCoachLoadAnalytics(
            UUID dispatcherId,
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone
    ) {
        verifyDispatcherAccess(dispatcherId, branchId);
        return analyticsPort.getCoachLoadAnalytics(branchId, dateFrom, dateTo, groupBy, timezone);
    }

    @Transactional(readOnly = true)
    public RetentionAnalyticsOutput getRetentionAnalytics(
            UUID dispatcherId,
            UUID branchId,
            AnalyticsCohortBy cohortBy,
            int periods,
            String timezone
    ) {
        verifyDispatcherAccess(dispatcherId, branchId);
        return analyticsPort.getRetentionAnalytics(branchId, cohortBy, periods, timezone);
    }

    @Transactional(readOnly = true)
    public SlaAnalyticsOutput getSlaAnalytics(
            UUID dispatcherId,
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone
    ) {
        verifyDispatcherAccess(dispatcherId, branchId);
        return analyticsPort.getSlaAnalytics(branchId, dateFrom, dateTo, groupBy, timezone);
    }

    private void verifyDispatcherAccess(UUID dispatcherId, UUID branchId) {
        boolean hasAccess = dispatcherBranchService.verifyBranchBelongsToDispatcher(dispatcherId, branchId);
        if (!hasAccess) {
            throw new BadRequestException("Dispatcher does not have access to branch", branchId);
        }
    }
}

