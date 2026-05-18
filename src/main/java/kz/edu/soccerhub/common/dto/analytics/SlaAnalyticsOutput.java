package kz.edu.soccerhub.common.dto.analytics;

public record SlaAnalyticsOutput(
        AnalyticsPeriodOutput period,
        LeadTimingOutput leadTiming,
        SlaBreachesOutput breaches
) {
}

