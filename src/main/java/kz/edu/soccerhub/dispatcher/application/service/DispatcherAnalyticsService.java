package kz.edu.soccerhub.dispatcher.application.service;

import kz.edu.soccerhub.common.dto.analytics.AnalyticsGroupBy;
import kz.edu.soccerhub.common.dto.analytics.AnalyticsResponseOutput;
import kz.edu.soccerhub.common.exception.AnalyticsForbiddenBranchException;
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
    public AnalyticsResponseOutput getFunnelAnalytics(
            UUID dispatcherId,
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone,
            UUID coachId,
            UUID groupId
    ) {
        verifyDispatcherAccess(dispatcherId, branchId);
        return analyticsPort.getFunnelAnalytics(branchId, dateFrom, dateTo, groupBy, timezone, coachId, groupId);
    }

    @Transactional(readOnly = true)
    public AnalyticsResponseOutput getCoachLoadAnalytics(
            UUID dispatcherId,
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone,
            UUID coachId,
            UUID groupId
    ) {
        verifyDispatcherAccess(dispatcherId, branchId);
        return analyticsPort.getCoachLoadAnalytics(branchId, dateFrom, dateTo, groupBy, timezone, coachId, groupId);
    }

    @Transactional(readOnly = true)
    public AnalyticsResponseOutput getRetentionAnalytics(
            UUID dispatcherId,
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone,
            UUID coachId,
            UUID groupId
    ) {
        verifyDispatcherAccess(dispatcherId, branchId);
        return analyticsPort.getRetentionAnalytics(branchId, dateFrom, dateTo, groupBy, timezone, coachId, groupId);
    }

    @Transactional(readOnly = true)
    public AnalyticsResponseOutput getSlaAnalytics(
            UUID dispatcherId,
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone,
            UUID coachId,
            UUID groupId
    ) {
        verifyDispatcherAccess(dispatcherId, branchId);
        return analyticsPort.getSlaAnalytics(branchId, dateFrom, dateTo, groupBy, timezone, coachId, groupId);
    }

    @Transactional(readOnly = true)
    public AnalyticsResponseOutput getLossReasonsAnalytics(
            UUID dispatcherId,
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone,
            UUID coachId,
            UUID groupId
    ) {
        verifyDispatcherAccess(dispatcherId, branchId);
        return analyticsPort.getLossReasonsAnalytics(branchId, dateFrom, dateTo, groupBy, timezone, coachId, groupId);
    }

    @Transactional(readOnly = true)
    public AnalyticsResponseOutput getKpiAnalytics(
            UUID dispatcherId,
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone,
            UUID coachId,
            UUID groupId
    ) {
        verifyDispatcherAccess(dispatcherId, branchId);
        return analyticsPort.getKpiAnalytics(branchId, dateFrom, dateTo, groupBy, timezone, coachId, groupId);
    }

    private void verifyDispatcherAccess(UUID dispatcherId, UUID branchId) {
        boolean hasAccess = dispatcherBranchService.verifyBranchBelongsToDispatcher(dispatcherId, branchId);
        if (!hasAccess) {
            throw new AnalyticsForbiddenBranchException(branchId);
        }
    }
}
