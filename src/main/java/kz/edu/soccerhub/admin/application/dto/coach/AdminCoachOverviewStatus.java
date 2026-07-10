package kz.edu.soccerhub.admin.application.dto.coach;

import kz.edu.soccerhub.common.exception.BadRequestException;

import java.util.Arrays;
import java.util.Locale;

public enum AdminCoachOverviewStatus {
    ALL,
    ACTIVE,
    INACTIVE,
    WITHOUT_GROUPS,
    OVERLOADED,
    TODAY;

    public static AdminCoachOverviewStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(status -> status.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unknown coach overview status", value));
    }
}
