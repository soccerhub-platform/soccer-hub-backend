package kz.edu.soccerhub.common.dto.analytics;

import java.util.List;

public record CoachLoadAnalyticsOutput(
        AnalyticsPeriodOutput period,
        List<CoachLoadRowOutput> rows
) {
}

