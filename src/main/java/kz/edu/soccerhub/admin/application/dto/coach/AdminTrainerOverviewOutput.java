package kz.edu.soccerhub.admin.application.dto.coach;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record AdminTrainerOverviewOutput(
        Trainer trainer,
        AdminTrainerListOutput.Load load,
        List<AttentionItem> attentionItems,
        List<GroupItem> groups,
        List<BranchItem> branches,
        Availability availability,
        NextSession nextSession,
        LastReport lastReport,
        int substitutionsThisWeek
) {
    public record Trainer(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String phone,
            String specialization,
            String accountStatus,
            String workStatus
    ) {}

    public record AttentionItem(
            String type,
            String severity,
            String title,
            String description,
            UUID entityId,
            Action action
    ) {}

    public record Action(
            String type,
            String label
    ) {}

    public record GroupItem(
            UUID groupId,
            String groupName,
            String role
    ) {}

    public record BranchItem(
            UUID id,
            String name
    ) {}

    public record Availability(
            List<String> days,
            String timeFrom,
            String timeTo,
            String timezone
    ) {}

    public record NextSession(
            UUID sessionId,
            UUID groupId,
            String groupName,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            String status
    ) {}

    public record LastReport(
            UUID sessionId,
            UUID groupId,
            String groupName,
            LocalDateTime submittedAt
    ) {}
}
