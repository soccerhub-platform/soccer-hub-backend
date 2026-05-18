package kz.edu.soccerhub.common.dto.analytics;

public record SlaPercentileOutput(
        int p50,
        int p75,
        int p90
) {
}

