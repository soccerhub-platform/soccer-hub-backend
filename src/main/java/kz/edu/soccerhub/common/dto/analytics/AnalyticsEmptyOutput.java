package kz.edu.soccerhub.common.dto.analytics;

public record AnalyticsEmptyOutput(
        boolean isEmpty,
        String reason
) {
}
