package kz.edu.soccerhub.common.dto.analytics;

import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;

import java.util.List;
import java.util.Map;

public record FunnelAnalyticsOutput(
        AnalyticsPeriodOutput period,
        Map<LeadStatus, Long> totals,
        FunnelRatesOutput rates,
        List<FunnelSeriesOutput> series
) {
}

