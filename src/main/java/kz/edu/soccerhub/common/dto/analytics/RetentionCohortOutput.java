package kz.edu.soccerhub.common.dto.analytics;

import java.util.List;

public record RetentionCohortOutput(
        String cohort,
        int baseCount,
        List<RetentionPointOutput> points
) {
}

