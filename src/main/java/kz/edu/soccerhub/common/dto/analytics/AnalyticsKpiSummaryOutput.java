package kz.edu.soccerhub.common.dto.analytics;

public record AnalyticsKpiSummaryOutput(
        long leadsNew,
        long leadsQualified,
        long leadsWon,
        long leadsLost,
        double winRate,
        int firstContactP50Minutes
) {
}
