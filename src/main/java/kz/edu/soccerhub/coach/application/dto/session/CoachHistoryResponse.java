package kz.edu.soccerhub.coach.application.dto.session;

import java.util.List;

@lombok.Builder
public record CoachHistoryResponse(
        List<CoachHistorySessionItem> content,
        long totalElements,
        int totalPages,
        int number,
        int size, java.util.Map<String,Long> summary, java.util.Map<String,Long> attendance,
        List<Workload> workload
) {
    @lombok.Builder public record Workload(String name,long value) {}
}
