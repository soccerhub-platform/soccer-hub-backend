package kz.edu.soccerhub.coach.application.dto.session;

import java.util.List;

public record CoachHistoryResponse(
        List<CoachHistorySessionItem> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {
}
