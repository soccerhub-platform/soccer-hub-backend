package kz.edu.soccerhub.common.dto.analytics;

import java.util.UUID;

public record CoachLoadRowOutput(
        UUID coachId,
        String coachName,
        int groups,
        int scheduledSlots,
        int completedSessions,
        int cancelledSessions,
        int activeStudents,
        int targetLoad,
        double utilizationRate
) {
}

