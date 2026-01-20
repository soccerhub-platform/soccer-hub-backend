package kz.edu.soccerhub.organization.application.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record GroupSummary(
        UUID groupId,
        int coachesCount,
        int sessionPerWeek,
        LocalDateTime nextSession,
        int studentsCount,
        int capacity,
        boolean scheduleActive
) {
}
