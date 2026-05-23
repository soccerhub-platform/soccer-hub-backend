package kz.edu.soccerhub.common.dto.analytics;

public record AnalyticsResponseOutput(
        AnalyticsMetaOutput meta,
        Object summary,
        Object series,
        Object totals,
        AnalyticsEmptyOutput empty
) {
}
