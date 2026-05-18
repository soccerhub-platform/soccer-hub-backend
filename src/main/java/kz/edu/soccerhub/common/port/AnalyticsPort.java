package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.analytics.AnalyticsCohortBy;
import kz.edu.soccerhub.common.dto.analytics.AnalyticsGroupBy;
import kz.edu.soccerhub.common.dto.analytics.CoachLoadAnalyticsOutput;
import kz.edu.soccerhub.common.dto.analytics.FunnelAnalyticsOutput;
import kz.edu.soccerhub.common.dto.analytics.RetentionAnalyticsOutput;
import kz.edu.soccerhub.common.dto.analytics.SlaAnalyticsOutput;

import java.time.LocalDate;
import java.util.UUID;

public interface AnalyticsPort {

    FunnelAnalyticsOutput getFunnelAnalytics(
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone
    );

    CoachLoadAnalyticsOutput getCoachLoadAnalytics(
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone
    );

    RetentionAnalyticsOutput getRetentionAnalytics(
            UUID branchId,
            AnalyticsCohortBy cohortBy,
            int periods,
            String timezone
    );

    SlaAnalyticsOutput getSlaAnalytics(
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone
    );
}

