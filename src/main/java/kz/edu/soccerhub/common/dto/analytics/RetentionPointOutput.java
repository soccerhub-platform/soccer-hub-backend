package kz.edu.soccerhub.common.dto.analytics;

public record RetentionPointOutput(
        int periodIndex,
        int retainedCount,
        double retentionRate
) {
}

