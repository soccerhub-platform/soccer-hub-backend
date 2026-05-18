package kz.edu.soccerhub.common.dto.analytics;

import java.util.List;

public record RetentionAnalyticsOutput(
        String timezone,
        List<RetentionCohortOutput> cohorts,
        List<RetentionGroupOutput> groups
) {
}

