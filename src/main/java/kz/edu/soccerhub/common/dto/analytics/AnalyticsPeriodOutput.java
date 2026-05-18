package kz.edu.soccerhub.common.dto.analytics;

import java.time.LocalDate;

public record AnalyticsPeriodOutput(
        LocalDate from,
        LocalDate to,
        AnalyticsGroupBy groupBy,
        String timezone
) {
}

