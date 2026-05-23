package kz.edu.soccerhub.common.dto.analytics;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record AnalyticsMetaOutput(
        LocalDate dateFrom,
        LocalDate dateTo,
        String timezone,
        Instant generatedAt,
        UUID branchId,
        Map<String, Object> filtersApplied
) {
}
