package kz.edu.soccerhub.admin.application.dto.session;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminGroupScheduleOverviewOutput(
        UUID groupId,
        LocalDate from,
        LocalDate to,
        Summary summary,
        Risk risk,
        List<Period> currentPeriods
) {
    public record Summary(int total, int planned, int inProgress, int completed, int cancelled) {}

    public record Risk(boolean hasConflicts, int conflictsCount, OffsetDateTime nextSessionAt) {}

    public record Period(
            String key,
            UUID coachId,
            String type,
            LocalDate startDate,
            LocalDate endDate,
            int sessionsPerWeek,
            List<Slot> slots,
            Capabilities capabilities
    ) {}

    public record Slot(UUID scheduleId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {}

    public record Capabilities(boolean canEdit, boolean canFinish) {}
}
