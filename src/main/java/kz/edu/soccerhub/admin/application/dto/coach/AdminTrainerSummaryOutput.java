package kz.edu.soccerhub.admin.application.dto.coach;

public record AdminTrainerSummaryOutput(
        int total,
        int active,
        int inactive,
        int withoutGroups,
        int overloaded,
        int withSessionsToday
) {
}
