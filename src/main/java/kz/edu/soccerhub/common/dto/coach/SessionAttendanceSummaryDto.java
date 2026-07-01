package kz.edu.soccerhub.common.dto.coach;

import java.util.UUID;

public record SessionAttendanceSummaryDto(
        UUID sessionId,
        long totalMarked,
        long presentLikeMarked
) {
}
