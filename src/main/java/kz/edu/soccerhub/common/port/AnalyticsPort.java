package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.analytics.AnalyticsGroupBy;
import kz.edu.soccerhub.common.dto.analytics.AnalyticsResponseOutput;

import java.time.LocalDate;
import java.util.UUID;

public interface AnalyticsPort {

    AnalyticsResponseOutput getFunnelAnalytics(
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone,
            UUID coachId,
            UUID groupId
    );

    AnalyticsResponseOutput getCoachLoadAnalytics(
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone,
            UUID coachId,
            UUID groupId
    );

    AnalyticsResponseOutput getRetentionAnalytics(
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone,
            UUID coachId,
            UUID groupId
    );

    AnalyticsResponseOutput getSlaAnalytics(
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone,
            UUID coachId,
            UUID groupId
    );

    AnalyticsResponseOutput getLossReasonsAnalytics(
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone,
            UUID coachId,
            UUID groupId
    );

    AnalyticsResponseOutput getKpiAnalytics(
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone,
            UUID coachId,
            UUID groupId
    );
}
