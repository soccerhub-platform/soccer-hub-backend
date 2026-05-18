package kz.edu.soccerhub.common.dto.analytics;

public record FunnelRatesOutput(
        double newToQualified,
        double qualifiedToTrialScheduled,
        double trialScheduledToWon,
        double winRateOnClosed
) {
}

