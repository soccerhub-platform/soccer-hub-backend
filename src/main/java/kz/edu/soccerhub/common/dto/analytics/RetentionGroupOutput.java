package kz.edu.soccerhub.common.dto.analytics;

import java.util.UUID;

public record RetentionGroupOutput(
        UUID groupId,
        String groupName,
        int totalSchedules,
        int cancelled,
        double retentionIndex
) {
}

